#!/usr/bin/env python3
"""Private local operations for PING. Standard library only; never prints credentials."""
from __future__ import annotations

import argparse
import hashlib
import csv
from datetime import datetime, timezone
import secrets
import threading
import uuid
import ipaddress
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from urllib.parse import parse_qsl, unquote, urlsplit

ROOT = Path(__file__).resolve().parents[2]


class OpsError(Exception):
    pass


def database_name(value: str) -> str:
    if not re.fullmatch(r'[A-Za-z0-9_]{1,64}', value or ''):
        raise OpsError('La base debe tener 1-64 letras ASCII, números o guion bajo.')
    return value


def db_config(url: str | None = None) -> dict:
    url = os.environ.get('DB_URL', '') if url is None else url
    if not url.startswith('jdbc:mysql://'):
        raise OpsError('Falta DB_URL o no es una URL JDBC MySQL de un solo servidor.')
    try:
        parsed = urlsplit(url[5:])
        port = parsed.port or 3306
    except ValueError:
        raise OpsError('DB_URL no tiene un servidor/puerto válido.') from None
    if parsed.username is not None or parsed.password is not None or parsed.fragment:
        raise OpsError('No pongas credenciales ni fragmentos dentro de DB_URL. Usa DB_USERNAME/DB_PASSWORD.')
    host = parsed.hostname or ''
    if not re.fullmatch(r'[A-Za-z0-9_.:-]+', host) or not 1 <= port <= 65535:
        raise OpsError('DB_URL requiere un solo host válido y puerto 1-65535.')
    name = database_name(unquote(parsed.path.lstrip('/')))
    options = {}
    for key, value in parse_qsl(parsed.query, keep_blank_values=True):
        key = key.lower()
        if key in options:
            raise OpsError('DB_URL repite opciones. Revísalas antes de continuar.')
        if (any(part in key for part in ('password', 'passwd', 'secret')) and key != 'trustcertificatekeystorepassword') or key in ('user', 'username', 'pwd'):
            # Login credentials in URLs risk being echoed by application connection logs.
            raise OpsError('DB_URL contiene una opción de credencial. Sepárala de la URL antes de usar estas herramientas.')
        options[key] = value
    mode = options.get('sslmode', '').upper()
    if not mode:
        if options.get('usessl', '').lower() == 'false':
            mode = 'DISABLED'
        elif options.get('verifyservercertificate', '').lower() == 'true':
            mode = 'VERIFY_CA'
        elif options.get('requiressl', '').lower() == 'true':
            mode = 'REQUIRED'
        else:
            mode = 'PREFERRED'
    if mode not in ('DISABLED', 'PREFERRED', 'REQUIRED', 'VERIFY_CA', 'VERIFY_IDENTITY'):
        raise OpsError('sslMode no es reconocido. No se reducirá la protección TLS.')
    return dict(host=host, port=port, database=name, ssl_mode=mode,
                public_key=options.get('allowpublickeyretrieval', '').lower() == 'true',
                java_truststore='trustcertificatekeystoreurl' in options)


def find_tool(name: str, explicit: str | None = None) -> str:
    if explicit:
        path = Path(explicit).expanduser().resolve()
        if path.is_file():
            return str(path)
        raise OpsError(f'No encontramos el ejecutable {name} indicado.')
    found = shutil.which(name)
    if found:
        return found
    if os.name == 'nt':
        # Known local installation directories; never downloads or executes a shell search.
        candidates = sorted(Path(os.environ.get('ProgramFiles', r'C:\Program Files')).glob(
            f'MySQL/MySQL Server 8.4/bin/{name}.exe'))
        if len(candidates) == 1:
            return str(candidates[0])
    raise OpsError(f'No encontramos {name}.exe. Indica su ruta instalada; no se instalará nada.')


def client_env(password: str | None = None) -> dict:
    password = os.environ.get('DB_PASSWORD') if password is None else password
    if password is None or password == '':
        raise OpsError('Falta DB_PASSWORD. Introdúcela de forma oculta en PowerShell.')
    env = os.environ.copy()
    env.pop('DB_PASSWORD', None)
    env['MYSQL_PWD'] = password  # Only this child; never an argv value, file, or printed command.
    return env


def mysql_args(config: dict, username: str, ssl_ca: str | None = None) -> list[str]:
    if not username or '\x00' in username or '\n' in username or '\r' in username:
        raise OpsError('Falta DB_USERNAME o contiene caracteres inválidos.')
    args = ['--no-defaults', '--no-login-paths', '--protocol=TCP',
            '--host=' + config['host'], '--port=' + str(config['port']), '--user=' + username,
            '--ssl-mode=' + config['ssl_mode'], '--default-character-set=utf8mb4']
    if ssl_ca:
        ca = Path(ssl_ca).expanduser().resolve()
        if not ca.is_file():
            raise OpsError('No encontramos el PEM indicado por -SslCa.')
        args.append('--ssl-ca=' + str(ca))
    elif config['ssl_mode'] in ('VERIFY_CA', 'VERIFY_IDENTITY') and config.get('java_truststore'):
        raise OpsError('El truststore Java no es un PEM del cliente MySQL. Indica -SslCa; se conserva TLS.')
    if config['public_key']:
        args.append('--get-server-public-key')
    return args


def mysql_error(stderr: bytes, operation: str, code: int) -> OpsError:
    # Do not reflect SQL, usernames, command lines or server text (may contain private values).
    match = re.search(rb'ERROR\s+(\d+)(?:\s+\(([A-Z0-9]+)\))?', stderr[:16384])
    number = match.group(1).decode('ascii') if match else 'sin código SQL'
    return OpsError(f'{operation} falló (salida {code}, {number}). Revisa servicio, permisos, TLS y credenciales; no se muestran secretos.')


class MySQL:
    def __init__(self, config: dict, executable: str | None = None, ssl_ca: str | None = None,
                 username: str | None = None, password: str | None = None):
        self.config = config
        self.executable = find_tool('mysql', executable)
        self.args = mysql_args(config, os.environ.get('DB_USERNAME', '') if username is None else username, ssl_ca) + ['--connect-timeout=5']
        self.env = client_env(password)

    def run(self, sql: str, database: str | None = None) -> bytes:
        args = [self.executable, *self.args, '--batch', '--skip-column-names', '--binary-as-hex', '--local-infile=0', '--binary-mode=1']
        if database:
            args.append('--database=' + database_name(database))
        try:
            result = subprocess.run(args, input=sql.encode('utf-8'), stdout=subprocess.PIPE,
                                    stderr=subprocess.PIPE, env=self.env, timeout=30)
        except subprocess.TimeoutExpired:
            raise OpsError('MySQL no respondió en 30 segundos. No se reinició ningún servicio.') from None
        if result.returncode:
            raise mysql_error(result.stderr, 'Consulta MySQL', result.returncode)
        return result.stdout

    def close(self):
        self.env.pop('MYSQL_PWD', None)


def safe_config(for_startup: bool = False) -> dict:
    missing = [name for name in ('DB_URL', 'DB_USERNAME', 'DB_PASSWORD') if not os.environ.get(name)]
    result = {'missing': missing}
    if os.environ.get('DB_URL'):
        config = db_config()
        # Hibernate/driver startup logs may echo a JDBC URL. Do not feed URL passwords to that process.
        if for_startup and any(any(part in key.lower() for part in ('password','passwd','secret'))
                               for key,_ in parse_qsl(urlsplit(os.environ['DB_URL'][5:]).query)):
            raise OpsError('El arranque no admite contraseñas dentro de DB_URL: los registros Java pueden mostrarla. Conserva tu arranque TLS existente o separa esa configuración de forma explícita; no se cambió la URL.')
        result.update({key: config[key] for key in ('host', 'port', 'database', 'ssl_mode')})
    return result


def check_database(args):
    config = db_config()
    client = MySQL(config, args.mysql, args.ssl_ca)
    try:
        text = client.run("SELECT VERSION(); SELECT DATABASE(); SHOW SESSION STATUS LIKE 'Ssl_cipher';", config['database']).decode('utf-8').splitlines()
        return dict(connection=True, server_version=text[0], database=config['database'], ssl_mode=config['ssl_mode'], tls_established=bool(text[2].partition('\t')[2]))
    finally:
        client.close()


# Recognize PING without requiring future tables before a user's first migration.
# Every table actually present is inventoried/dumped/compared, including older schemas.
REQUIRED_TABLES = {'flyway_schema_history','events','participants'}


def outside_repo(path: Path) -> Path:
    path = path.expanduser().resolve()
    if path == ROOT or ROOT in path.parents:
        raise OpsError('El destino/origen privado debe estar FUERA del repositorio, también al resolver enlaces.')
    return path


def private_directory(parent: Path) -> Path:
    parent = outside_repo(parent)
    parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    directory = parent / ('ping_backup_' + datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ') + '_' + uuid.uuid4().hex[:12])
    directory.mkdir(mode=0o700)  # exclusive: never reuse or overwrite another backup directory
    if os.name == 'nt':
        result = subprocess.run(['whoami.exe','/user','/fo','csv','/nh'],stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=10)
        try:
            sid = next(csv.reader(result.stdout.decode('utf-8',errors='replace').splitlines()))[-1]
        except (StopIteration, IndexError):
            raise OpsError('No se pudo identificar tu SID para proteger la carpeta nueva.') from None
        if result.returncode or not re.fullmatch(r'S-1-[0-9-]+',sid):
            raise OpsError('No se pudo identificar tu SID para proteger la carpeta nueva.')
        secured = subprocess.run(['icacls.exe',str(directory),'/inheritance:r','/grant:r','*'+sid+':(OI)(CI)F','/Q'],stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=10)
        if secured.returncode:
            raise OpsError('No se pudo limitar el acceso a la carpeta NUEVA. Usa un destino privado con ACL; no se generó el dump.')
    return directory


def private_write(path: Path, data: bytes):
    # Never truncates an existing file, even if a timestamp/path is accidentally reused.
    with os.fdopen(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600),'wb') as stream:
        stream.write(data);stream.flush();os.fsync(stream.fileno())


def json_write(path: Path, value):
    private_write(path,(json.dumps(value,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))


def stream_hash(stream) -> str:
    digest = hashlib.sha256()
    for chunk in iter(lambda: stream.read(1024*1024),b''):
        digest.update(chunk)
    return digest.hexdigest()


def file_hash(path: Path) -> str:
    with path.open('rb') as stream:
        return stream_hash(stream)


def streamed_query_hash(client: MySQL, sql: str, database: str) -> str:
    # Stream escaped batch rows, including hex blobs, without writing plaintext verification data.
    command = [client.executable,*client.args,'--batch','--skip-column-names','--binary-as-hex',
               '--local-infile=0','--binary-mode=1','--database='+database_name(database)]
    process = subprocess.Popen(command,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=client.env)
    stderr = bytearray()
    def drain():
        for data in iter(lambda: process.stderr.read(4096),b''):
            if len(stderr)<16384:stderr.extend(data[:16384-len(stderr)])
    reader = threading.Thread(target=drain,daemon=True);reader.start()
    expired = threading.Event()
    def stop():
        expired.set();process.kill()
    timer=threading.Timer(300,stop);timer.start()
    digest=hashlib.sha256()
    try:
        process.stdin.write(sql.encode('utf-8'));process.stdin.close()
        for chunk in iter(lambda: process.stdout.read(1024*1024),b''):digest.update(chunk)
        code=process.wait();reader.join()
        if expired.is_set():raise OpsError('La verificación MySQL excedió 300 segundos; no se declara correcta.')
        if code:raise mysql_error(bytes(stderr),'Verificación MySQL',code)
        return digest.hexdigest()
    finally:
        timer.cancel()
        if process.poll() is None:process.kill();process.wait()
        process.stdout.close();process.stderr.close()


def inventory(client: MySQL, database: str) -> dict:
    rows=client.run("SELECT TABLE_NAME,ENGINE,TABLE_TYPE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME;",database).decode('utf-8').splitlines()
    tables={}
    for row in rows:
        table,engine,kind=row.split('\t')
        database_name(table)
        if kind!='BASE TABLE' or engine.upper()!='INNODB':
            raise OpsError('Este respaldo verificado admite tablas InnoDB de PING; se detectó otro tipo. No se presume consistencia.')
        keys=client.run("SELECT COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"+table+"' AND CONSTRAINT_NAME='PRIMARY' ORDER BY ORDINAL_POSITION;",database).decode('utf-8').splitlines()
        if not keys:raise OpsError('Hay una tabla sin clave primaria; no se puede comparar su orden de forma fiable.')
        order=','.join('`'+database_name(key)+'`' for key in keys)
        count=int(client.run('SELECT COUNT(*) FROM `'+table+'`;',database).strip())
        definition=client.run('SHOW CREATE TABLE `'+table+'`;',database)
        columns=client.run("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"+table+"' ORDER BY ORDINAL_POSITION;",database).decode('utf-8').splitlines()
        # Explicit null marker + hex value: NULL and the text 'NULL', tabs and newlines cannot collide.
        encoded=','.join("IF(`"+database_name(col)+"` IS NULL,'N',CONCAT('V',HEX(CAST(`"+col+"` AS BINARY))))" for col in columns)
        tables[table]={'rows':count,'schema_sha256':hashlib.sha256(definition).hexdigest(),
                       'data_sha256':streamed_query_hash(client,'SELECT '+encoded+' FROM `'+table+'` ORDER BY '+order+';',database)}
    if not REQUIRED_TABLES.issubset(tables):
        raise OpsError('La base no contiene la estructura mínima reconocible de PING (Flyway, eventos y participantes).')
    return tables


def backup_database(args):
    destination=outside_repo(Path(args.destination))
    dumper=find_tool('mysqldump',args.mysqldump)
    config=db_config();client=MySQL(config,args.mysql,args.ssl_ca)
    try:
        version=client.run('SELECT VERSION();',config['database']).decode('utf-8').strip()
        before=inventory(client,config['database'])
        folder=private_directory(destination)
        dump=folder/('ping_'+config['database']+'_'+datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')+'.sql')
        print('Destino privado NUEVO: '+str(folder),flush=True)
        options=mysql_args(config,os.environ.get('DB_USERNAME',''),args.ssl_ca)
        command=[dumper,*options,'--single-transaction','--quick','--hex-blob','--no-tablespaces',
                 '--set-gtid-purged=OFF','--column-statistics=0','--skip-add-drop-table','--skip-add-locks',
                 '--routines','--events','--triggers',config['database']]
        with os.fdopen(os.open(dump,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600),'wb') as stream:
            try:result=subprocess.run(command,stdout=stream,stderr=subprocess.PIPE,env=client.env,timeout=600)
            except subprocess.TimeoutExpired:raise OpsError('mysqldump excedió600s. El archivo privado parcial se conserva; no tiene certificado de respaldo válido.') from None
            stream.flush();os.fsync(stream.fileno())
        if result.returncode:raise mysql_error(result.stderr,'mysqldump; archivo privado parcial conservado',result.returncode)
        if dump.stat().st_size==0:raise OpsError('mysqldump devolvió un archivo vacío. No se declara respaldo correcto.')
        after=inventory(client,config['database'])
        if before!=after:
            raise OpsError('La base cambió durante el respaldo. Archivo privado conservado sin manifiesto válido; detén escrituras/DDL de forma explícita y repite en otro respaldo.')
        digest=file_hash(dump)
        source_schema=client.run("SELECT version FROM flyway_schema_history WHERE success=1 AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1;",config['database']).decode('utf-8').strip()
        metadata={'format':'ping-backup-v1','source_database':config['database'],'server_version':version,'source_schema_version':source_schema,
                  'created_utc':datetime.now(timezone.utc).isoformat(),'bytes':dump.stat().st_size,
                  'sha256':digest,'tables':before,'source_stable_observed':True}
        private_write(Path(str(dump)+'.sha256'),(digest+'  '+dump.name+'\n').encode('utf-8'))
        json_write(Path(str(dump)+'.json'),metadata)
        return {'backup':str(dump),'sha256':digest,'bytes':metadata['bytes'],'tables':len(before),
                'status':'capturado_y_comparado; restaurabilidad pendiente de ensayo de restauración'}
    finally:client.close()


def validate_backup(path: Path) -> tuple[Path,dict]:
    path=outside_repo(path)
    if not path.is_file() or path.suffix.lower()!='.sql' or path.stat().st_size==0:
        raise OpsError('Selecciona un archivo .sql no vacío de Backup-Ping, fuera del repositorio.')
    try:
        checksum=Path(str(path)+'.sha256').read_text(encoding='utf-8').strip()
        match=re.fullmatch(r'([a-f0-9]{64})  (.+)',checksum)
        metadata=json.loads(Path(str(path)+'.json').read_text(encoding='utf-8'))
    except (OSError,ValueError):raise OpsError('Falta checksum/manifiesto válido junto al SQL. No se importará.') from None
    if not match or match.group(2)!=path.name or file_hash(path)!=match.group(1):
        raise OpsError('El checksum SHA-256 no coincide. No se conectó ni importó nada.')
    if (not isinstance(metadata,dict) or metadata.get('format')!='ping-backup-v1' or metadata.get('sha256')!=match.group(1)
        or metadata.get('bytes')!=path.stat().st_size or metadata.get('source_stable_observed') is not True
        or not isinstance(metadata.get('tables'),dict) or not REQUIRED_TABLES.issubset(metadata['tables'])):
        raise OpsError('El manifiesto no corresponde a un respaldo completo comprobable de PING.')
    database_name(metadata.get('source_database',''))
    for name,row in metadata['tables'].items():
        database_name(name)
        if (not isinstance(row,dict) or not isinstance(row.get('rows'),int) or row['rows']<0
            or any(not re.fullmatch(r'[a-f0-9]{64}',str(row.get(field,''))) for field in ('schema_sha256','data_sha256'))):
            raise OpsError('El manifiesto contiene una estructura de verificación inválida.')
    return path,metadata


def restore_database(args):
    dump,metadata=validate_backup(Path(args.backup))
    target=database_name(args.target)
    if not target.startswith('ping_restore_exclusive_') or target.lower()==metadata['source_database'].lower():
        raise OpsError('El destino debe ser NUEVO, distinto del origen y empezar por ping_restore_exclusive_.')
    config=db_config()
    print('Origen del respaldo: '+metadata['source_database']+' | archivo: '+str(dump),flush=True)
    print('Servidor destino: '+config['host']+':'+str(config['port'])+' | base NUEVA: '+target,flush=True)
    print('No se cambia DB_URL ni la configuración habitual de PING. No se borra ninguna base.',flush=True)
    if (args.confirm_target or input('Escribe el nombre exacto de la base NUEVA para confirmar: '))!=target:
        raise OpsError('Confirmación distinta del destino. No se conectó ni importó nada.')
    client=MySQL(config,args.mysql,args.ssl_ca);created=False;account_created=False;limited=None
    token=secrets.token_hex(10);user='ping_restore_'+token[:16];password='Aa1!'+secrets.token_urlsafe(36);host=''
    record={'target':target,'source_database':metadata['source_database'],'status':'preflight','backup_sha256':metadata['sha256']}
    report=dump.parent/('restore_'+target+'_'+uuid.uuid4().hex[:8]+'.json')
    try:
        exists=client.run("SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE LOWER(SCHEMA_NAME)=LOWER('"+target+"');")
        if int(exists.strip())!=0:raise OpsError('El destino YA EXISTE. Se rechaza, aunque esté vacío; no se importó ni borró nada.')
        host=client.run("SELECT SUBSTRING_INDEX(USER(),'@',-1);").decode('utf-8').strip()
        if not re.fullmatch(r'[A-Za-z0-9_.:%-]+',host) or host=='%':raise OpsError('No se pudo limitar el usuario temporal a tu host de conexión.')
        # Without IF NOT EXISTS: a race for the name fails instead of importing over another database.
        client.run('CREATE DATABASE `'+target+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;');created=True
        record['status']='database_created';record['temporary_account']=user+'@'+host
        # Password is generated safe ASCII, sent only on stdin. No general/global grants or FILE.
        client.run("CREATE USER '"+user+"'@'"+host+"' IDENTIFIED BY '"+password+"';");account_created=True
        client.run("GRANT ALL PRIVILEGES ON `"+target+"`.* TO '"+user+"'@'"+host+"';")
        limited=MySQL(config,args.mysql,args.ssl_ca,username=user,password=password)
        command=[limited.executable,*limited.args,'--database='+target,'--binary-mode=1','--local-infile=0','--batch']
        with dump.open('rb') as stream:
            # Hash the same opened handle that will feed mysql; replacing a pathname cannot switch input.
            if stream_hash(stream)!=metadata['sha256']:raise OpsError('El SQL cambió después de validar. No se importó; la base NUEVA se conserva.')
            stream.seek(0)
            try:result=subprocess.run(command,stdin=stream,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE,env=limited.env,timeout=600)
            except subprocess.TimeoutExpired:raise OpsError('Importación excedió600s. La base NUEVA parcial se conserva; no la reutilices como destino.') from None
            stream.seek(0)
            if stream_hash(stream)!=metadata['sha256']:raise OpsError('El archivo cambió durante la importación. La base NUEVA se conserva sin declarar éxito.')
        if result.returncode:raise mysql_error(result.stderr,'Importación en base nueva parcial conservada',result.returncode)
        actual=inventory(limited,target)
        if actual!=metadata['tables']:raise OpsError('Importó, pero estructura/filas/huellas no coinciden. La base NUEVA se conserva y NO se declara verificada.')
        record['status']='restored_and_verified';record['tables']=len(actual)
        return {'target':target,'status':record['status'],'tables':len(actual),
                'verification':'estructura, recuentos y SHA-256 de TODAS las filas ordenadas, incluidos blobs/estados/saldos',
                'report':str(report),'habitual_config_changed':False}
    finally:
        if limited:limited.close()
        cleanup_failed=False
        if account_created:
            try:client.run("DROP USER '"+user+"'@'"+host+"';");record['temporary_account_removed']=True
            except OpsError:cleanup_failed=True;record['temporary_account_removed']=False
        client.close();password=''
        if created:
            if record['status']!='restored_and_verified':record['status']='partial_new_database_retained'
            json_write(report,record)
            print('Registro privado del ensayo: '+str(report),flush=True)
            if record['status']!='restored_and_verified':print('Base NUEVA parcial conservada: '+target+'; no se borró ni reutilizó.',flush=True)
        if cleanup_failed:raise OpsError('No se pudo retirar el usuario temporal '+user+'@'+host+'. La base nueva y el registro se conservan; requiere retirada privilegiada explícita.')


def client_versions(args):
    result={}
    env=os.environ.copy();env.pop('DB_PASSWORD',None);env.pop('MYSQL_PWD',None)
    for name,explicit in [('mysql',args.mysql),('mysqldump',args.mysqldump)]:
        try:
            tool=find_tool(name,explicit)
            version=subprocess.run([tool,'--no-defaults','--no-login-paths','--version'],stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=env,timeout=10)
            lines=version.stdout.decode('utf-8',errors='replace').splitlines()
            if version.returncode or not lines:result[name]={'available':False,'reason':'no respondió a --version'}
            else:result[name]={'available':True,'version':lines[0][:256]}
        except OpsError:result[name]={'available':False,'reason':'no encontrado; indica ruta instalada'}
    return result


def parser():
    p = argparse.ArgumentParser(description=__doc__)
    sub = p.add_subparsers(dest='command', required=True)
    config_parser=sub.add_parser('config');config_parser.add_argument('--for-startup',action='store_true')
    versions=sub.add_parser('versions');versions.add_argument('--mysql');versions.add_argument('--mysqldump')
    check = sub.add_parser('check-db')
    check.add_argument('--mysql')
    check.add_argument('--ssl-ca')
    backup = sub.add_parser('backup')
    backup.add_argument('--destination',default=str(Path(os.environ.get('LOCALAPPDATA',Path.home()))/'PING'/'backups'))
    backup.add_argument('--mysql');backup.add_argument('--mysqldump');backup.add_argument('--ssl-ca')
    restore = sub.add_parser('restore')
    restore.add_argument('--backup',required=True);restore.add_argument('--target',required=True)
    restore.add_argument('--mysql');restore.add_argument('--ssl-ca');restore.add_argument('--confirm-target')
    return p


def main(argv=None):
    # One operational entry point: Start-Ping.ps1 and the root double-click files use this.
    arguments = sys.argv[1:] if argv is None else argv
    if arguments and arguments[0] in ('start', 'stop', 'configure', 'forget', 'serve', 'firewall', 'trust-pc'):
        from ping_launcher import main as launch
        return launch(arguments)
    args = parser().parse_args(argv)
    try:
        actions = {'versions':client_versions,'config':lambda options:safe_config(options.for_startup),'check-db':check_database,'backup':backup_database,'restore':restore_database}
        result = actions[args.command](args)
        print(json.dumps(result, ensure_ascii=False))
        return 0
    except OpsError as error:
        print('PING: ' + str(error), file=sys.stderr)
        return 1
    except (OSError, ValueError, EOFError, subprocess.TimeoutExpired):
        print('PING: fallo local de archivo/proceso. Comprueba rutas y permisos; no se muestran datos privados.', file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
