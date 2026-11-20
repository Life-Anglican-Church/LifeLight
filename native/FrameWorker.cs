using System;using System.Collections.Generic;using System.Collections.Concurrent;using System.Threading;using System.Diagnostics;using System.Web.Script.Serialization;
static class Program{
 public static readonly JavaScriptSerializer Json=new JavaScriptSerializer{MaxJsonLength=2000000};
 static int Main(string[] args){if(args.Length==1&&args[0]=="--validate-frame"){try{Frame(Json.Deserialize<Dictionary<string,object>>(Console.ReadLine()));Console.WriteLine("valid");return 0;}catch{return 2;}}if(args.Length==2&&args[0]=="--prepare"){try{VendorFiles.FromZip(args[1]);return 0;}catch{return 2;}}
 var queue=new BlockingCollection<string>();new Thread(()=>{string s;try{while((s=Console.ReadLine())!=null){if(s.Length>20000)break;queue.Add(s);}}finally{queue.Add("{\"op\":\"exit\"}");}}){IsBackground=true}.Start();NativeSdk sdk=null;bool active=false;var clock=Stopwatch.StartNew();long last=0;
 try{while(true){string line;if(!queue.TryTake(out line,100)){if(active&&clock.ElapsedMilliseconds-last>3000){sdk.Close();active=false;Reply(0,false,"Heartbeat lost; SDK closed. Physical state unconfirmed.",null);}continue;}
 int id=0;try{var c=Json.Deserialize<Dictionary<string,object>>(line);if(c.ContainsKey("id"))id=Convert.ToInt32(c["id"]);string op=Convert.ToString(c["op"]);if(op=="exit")break;
 if(op=="open"){if(active)throw new InvalidOperationException("Already open");var frame=Frame(c);sdk=new NativeSdk(VendorFiles.Folder);int count=sdk.Count();if(count!=1)throw new InvalidOperationException("Expected exactly one USB controller; found "+count);if(!sdk.Open(0,1))throw new InvalidOperationException("SDK open rejected");active=true;last=clock.ElapsedMilliseconds;if(!sdk.Write(0,0,frame))throw new InvalidOperationException("Initial frame rejected");var fw=sdk.ReadData(0,true);Reply(id,true,null,new{firmware=fw==null?null:String.Join(".",fw),universes=sdk.Universes(0),connected=sdk.Connected(0)});}
 else if(op=="frame"){if(!active)throw new InvalidOperationException("Output not open");var frame=Frame(c);if(!sdk.Write(0,0,frame))throw new InvalidOperationException("Frame rejected");last=clock.ElapsedMilliseconds;Reply(id,true,null,null);}
 else if(op=="close"){if(sdk!=null)sdk.Close();active=false;Reply(id,true,null,null);}
 else throw new InvalidOperationException("Unknown command");
 }catch(Exception e){if(active)try{sdk.Close();}catch{}active=false;Reply(id,false,e is InvalidOperationException?e.Message:e.GetType().Name,null);}}
 }finally{if(sdk!=null)try{sdk.Close();}catch{}}return 0;}
 static byte[] Frame(Dictionary<string,object> c){if(!c.ContainsKey("frame"))throw new ArgumentException();var a=c["frame"] as System.Collections.IList;if(a==null||a.Count!=512)throw new ArgumentException();var b=new byte[512];for(int i=0;i<512;i++){if(!(a[i] is int))throw new ArgumentException();int v=(int)a[i];if(v<0||v>255)throw new ArgumentException();b[i]=(byte)v;}return b;}
 static void Reply(int id,bool ok,string error,object data){Console.WriteLine(Json.Serialize(new{id=id,ok=ok,error=error,data=data}));Console.Out.Flush();}
}
