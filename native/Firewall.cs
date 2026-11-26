using System;
static class FirewallTool{
 [STAThread]static int Main(string[] args){if(args.Length!=1)return 2;if(args[0]=="--configure-firewall"||args[0]=="--remove-firewall")return Deployment.ConfigureFirewall(args[0]=="--remove-firewall")?0:2;if(args[0]=="--request-firewall")return Deployment.RequestFirewall(System.Reflection.Assembly.GetExecutingAssembly().Location,false,s=>{})?0:2;return 2;}
}
