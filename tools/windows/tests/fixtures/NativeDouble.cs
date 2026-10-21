// Test-only local executable; never opens a socket or a database.
using System;
using System.Linq;
using System.Text;
class NativeDouble {
    static int Main(string[] args) {
        Console.OutputEncoding = new UTF8Encoding(false);
        if (args.Length > 0 && args[0] == "__argv__") {
            foreach (string value in args.Skip(1))
                Console.WriteLine("ARG:" + Convert.ToBase64String(Encoding.UTF8.GetBytes(value)));
            return 0;
        }
        if (args.SequenceEqual(new[] { "-version" })) {
            Console.Error.WriteLine("openjdk version \"21.0.1\" (native double)");
            return 0;
        }
        string password = Environment.GetEnvironmentVariable("MYSQL_PWD");
        if (Environment.GetEnvironmentVariable("PING_TEST_CLIENT_FAIL") == "1") {
            Console.Error.WriteLine("ERROR 1045 (28000): synthetic private diagnostic " + password);
            return 7;
        }
        if (String.IsNullOrEmpty(password) || Environment.GetEnvironmentVariable("DB_PASSWORD") != null ||
                args.Any(a => a.Contains(password) || a.StartsWith("--password")) ||
                !args.Contains("--ssl-ca=" + Environment.GetEnvironmentVariable("PING_TEST_CA")) ||
                !args.Contains("--database=ping_ci_fixture") ||
                !Console.In.ReadToEnd().Contains("SELECT VERSION()")) return 9;
        Console.WriteLine("8.4.11-double\nping_ci_fixture\nSsl_cipher\tSYNTHETIC_TLS");
        return 0;
    }
}
