"""Focused checks for credential handling, URL interpretation and operational failure paths."""
import importlib.util
import os
from pathlib import Path
import subprocess
import argparse
import contextlib
import hashlib
import io
import json
import tempfile
import sys
import unittest
from unittest import mock

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('ping_ops', Path(__file__).resolve().parents[1] / 'ping_ops.py')
ops = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ops)

class ConfigurationTests(unittest.TestCase):
    def test_existing_jdbc_options_are_not_rewritten_and_tls_is_preserved(self):
        result = ops.db_config('jdbc:mysql://localhost:3308/ping?sslMode=VERIFY_IDENTITY&serverTimezone=America/Bogota')
        self.assertEqual((result['host'], result['port'], result['database'], result['ssl_mode']), ('localhost',3308,'ping','VERIFY_IDENTITY'))
        self.assertEqual(ops.db_config('jdbc:mysql://127.0.0.1/ping?useSSL=false')['ssl_mode'], 'DISABLED')
        self.assertEqual(ops.db_config('jdbc:mysql://localhost/ping')['ssl_mode'], 'PREFERRED')
    def test_secrets_and_ambiguous_or_unsafe_urls_are_rejected_without_echo(self):
        for url in ['jdbc:mysql://user:PRIVATE@localhost/ping', 'jdbc:mysql://localhost/ping?password=PRIVATE',
                    'jdbc:mysql://localhost/ping?sslMode=PREFERRED&sslMode=DISABLED', 'jdbc:mysql://a,b/ping',
                    'jdbc:mysql://localhost/ping%2Fother', 'jdbc:mysql://localhost/ping#PRIVATE']:
            with self.subTest(url=url):
                with self.assertRaises(ops.OpsError) as caught:ops.db_config(url)
                self.assertNotIn('PRIVATE', str(caught.exception))
    def test_credentials_only_go_into_child_environment(self):
        with mock.patch.dict(os.environ, {'DB_USERNAME':'tester','DB_PASSWORD':'SECRET unique test only'},clear=True):
            args=ops.mysql_args(ops.db_config('jdbc:mysql://localhost/ping'),'tester')
            child=ops.client_env()
            self.assertNotIn('SECRET unique test only',' '.join(args))
            self.assertEqual(child['MYSQL_PWD'],'SECRET unique test only')
            self.assertNotIn('DB_PASSWORD',child)
            self.assertEqual(os.environ['DB_PASSWORD'],'SECRET unique test only')
    def test_java_truststore_does_not_silently_downgrade_cli_tls(self):
        c=ops.db_config('jdbc:mysql://localhost/ping?sslMode=VERIFY_CA&trustCertificateKeyStoreUrl=file:/trust.p12')
        with self.assertRaises(ops.OpsError):ops.mysql_args(c,'test')
    def test_database_check_propagates_failure_without_server_sql_or_secret(self):
        with mock.patch.dict(os.environ, {'DB_USERNAME':'tester','DB_PASSWORD':'PRIVATE'},clear=True), mock.patch.object(ops,'find_tool',return_value='mysql'), mock.patch.object(ops.subprocess,'run',return_value=subprocess.CompletedProcess([],1,b'',b'ERROR 1045 (28000): private server SQL PRIVATE')):
            client=ops.MySQL(ops.db_config('jdbc:mysql://localhost/ping'))
            with self.assertRaises(ops.OpsError) as caught:client.run('SELECT 1')
            self.assertIn('1045',str(caught.exception));self.assertNotIn('PRIVATE',str(caught.exception));client.close();self.assertNotIn('MYSQL_PWD',client.env)
    def test_config_presence_does_not_echo_username_password_or_url_credentials(self):
        with mock.patch.dict(os.environ,{'DB_URL':'jdbc:mysql://localhost/ping','DB_USERNAME':'private-user','DB_PASSWORD':'private-password'},clear=True):
            result=str(ops.safe_config());self.assertNotIn('private-',result);self.assertIn('PREFERRED',result)


class BackupRestoreTests(unittest.TestCase):
    def fixture(self, parent):
        path=Path(parent)/'respaldo ficticio á.sql';data=b'-- fictitious test SQL only\n'
        path.write_bytes(data);digest=hashlib.sha256(data).hexdigest()
        Path(str(path)+'.sha256').write_text(digest+'  '+path.name+'\n',encoding='utf-8')
        meta={'format':'ping-backup-v1','source_database':'ping_ficticio','bytes':len(data),'sha256':digest,
              'source_stable_observed':True,'tables':{name:{'rows':0,'schema_sha256':'a'*64,'data_sha256':'b'*64} for name in ops.REQUIRED_TABLES}}
        Path(str(path)+'.json').write_text(json.dumps(meta),encoding='utf-8');return path,meta
    def test_sql_and_manifest_integrity_accept_binary_hash_then_reject_tampering_before_connect(self):
        with tempfile.TemporaryDirectory() as directory:
            path,meta=self.fixture(directory);self.assertEqual(ops.validate_backup(path)[1],meta)
            path.write_bytes(path.read_bytes()+b'ALTER DATA')
            with mock.patch.object(ops,'MySQL') as client:
                with self.assertRaises(ops.OpsError):ops.restore_database(argparse.Namespace(backup=str(path),target='ping_restore_exclusive_test',confirm_target='ping_restore_exclusive_test'))
                client.assert_not_called()
    def test_invalid_manifest_and_existing_file_are_not_accepted_or_overwritten(self):
        with tempfile.TemporaryDirectory() as directory:
            path,_=self.fixture(directory);Path(str(path)+'.json').write_text('[]',encoding='utf-8')
            with self.assertRaises(ops.OpsError):ops.validate_backup(path)
            before=path.read_bytes()
            with self.assertRaises(FileExistsError):ops.private_write(path,b'replace')
            self.assertEqual(path.read_bytes(),before)
    def test_repository_destinations_are_rejected_including_resolved_links(self):
        with tempfile.TemporaryDirectory() as directory:
            repo=Path(directory)/'repo';repo.mkdir()
            with mock.patch.object(ops,'ROOT',repo):
                with self.assertRaises(ops.OpsError):ops.private_directory(repo/'backups')
                self.assertFalse((repo/'backups').exists())
    def test_existing_destination_is_rejected_without_create_import_or_drop(self):
        target='ping_restore_exclusive_existing'
        with tempfile.TemporaryDirectory() as directory:
            path,_=self.fixture(directory)
            client=mock.Mock();client.run.return_value=b'1\n'
            args=argparse.Namespace(backup=str(path),target=target,confirm_target=target,mysql=None,ssl_ca=None)
            with mock.patch.dict(os.environ,{'DB_URL':'jdbc:mysql://localhost/ping_ficticio'},clear=True), mock.patch.object(ops,'MySQL',return_value=client),contextlib.redirect_stdout(io.StringIO()):
                with self.assertRaises(ops.OpsError) as error:ops.restore_database(args)
                self.assertIn('YA EXISTE',str(error.exception));self.assertEqual(client.run.call_count,1);client.close.assert_called_once()
    def test_confirmation_and_target_prefix_are_required_before_connect(self):
        with tempfile.TemporaryDirectory() as directory:
            path,_=self.fixture(directory)
            for target,confirmation in [('ping_ficticio','ping_ficticio'),('ping_restore_exclusive_test','other')]:
                args=argparse.Namespace(backup=str(path),target=target,confirm_target=confirmation,mysql=None,ssl_ca=None)
                with mock.patch.dict(os.environ,{'DB_URL':'jdbc:mysql://localhost/ping_ficticio'},clear=True),mock.patch.object(ops,'MySQL') as client,contextlib.redirect_stdout(io.StringIO()):
                    with self.assertRaises(ops.OpsError):ops.restore_database(args)
                    client.assert_not_called()
    def test_mysqldump_options_do_not_inherit_mysql_only_connect_timeout(self):
        common=ops.mysql_args(ops.db_config('jdbc:mysql://localhost/ping'),'test')
        self.assertFalse(any(arg.startswith('--connect-timeout') for arg in common))
        self.assertEqual(common[:2],['--no-defaults','--no-login-paths'])
    def test_private_configuration_has_no_password_file(self):
        with tempfile.TemporaryDirectory() as directory:
            created=ops.private_directory(Path(directory));ops.private_write(created/'test.json',b'{}')
            self.assertEqual([p.name for p in created.iterdir()],['test.json'])
            if os.name=='posix':self.assertEqual(created.stat().st_mode & 0o777,0o700)

if __name__=='__main__':unittest.main()
