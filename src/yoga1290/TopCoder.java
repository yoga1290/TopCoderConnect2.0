import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

public class TopCoder
	{
	
	public static String readFromURL(String uri)
	{
		String res="";
		if(uri.indexOf("http://community.topcoder.com/tc?module=BasicData&c=dd_round_results&rd=")!=-1)
		{
			try{
			com.google.appengine.api.datastore.Text cache=(com.google.appengine.api.datastore.Text)(DatastoreServiceFactory.getDatastoreService().get(KeyFactory.createKey("topcoder_cache", uri.substring(72) )).getProperty("data"));
			return cache.getValue();
			}catch(Exception e){}
		}
		try
		{	
			URL url = new URL(uri);
	
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setReadTimeout(59);
	        InputStream in=connection.getInputStream();
	        byte buff[]=new byte[in.available()];
            int ch;
            while((ch=in.read(buff))!=-1)
            		res+=new String(buff,0,ch);
		}catch(Exception e)
		{
			res=e.getMessage();
		}
		return res;		
	}
	public static String getCoderIDfromDB(String fbid)
	{
		try{
			return (String)(DatastoreServiceFactory.getDatastoreService().get(KeyFactory.createKey("topcoder", fbid)).getProperty("topcoderid"));
		}catch(Exception e){return null;}
	}
	public static String getCoderLastSRM(String fbid)
	{
		try{
			return (String)(DatastoreServiceFactory.getDatastoreService().get(KeyFactory.createKey("topcoder", fbid)).getProperty("lastsrm"));
		}catch(Exception e){return null;}
	}
	public static void setCoderLastSRM(String fbid,String roundID)
	{
		try{
			DatastoreServiceFactory.getDatastoreService().get(KeyFactory.createKey("topcoder", fbid) ).setProperty("lastsrm",roundID);
		}catch(Exception e){}//return null;}
	}
	
	public static String getLastSRMResult(String coderID)
	{
		String txt=readFromURL("http://community.topcoder.com/tc?module=BasicData&c=dd_rating_history&cr="+coderID);
		txt=txt.substring(txt.lastIndexOf("<row>")+5,txt.lastIndexOf("</row>"));
		
		String roundID=txt.substring(txt.lastIndexOf("<round_id>")+10,txt.lastIndexOf("</round_id>"));
		
		String roundData=readFromURL("http://community.topcoder.com/tc?module=BasicData&c=dd_round_results&rd="+roundID);
		roundData=roundData.substring(roundData.indexOf("<coder_id>"+coderID+"</coder_id>"));
		roundData=roundData.substring(0,roundData.indexOf("</row>"));
		
		int oldR=Integer.parseInt( txt.substring(txt.lastIndexOf("<old_rating>")+12,txt.lastIndexOf("</old_rating>")) );
		int newR=Integer.parseInt( txt.substring(txt.lastIndexOf("<new_rating>")+12,txt.lastIndexOf("</new_rating>")) );
		String res=txt.substring(txt.lastIndexOf("<handle>")+8,txt.lastIndexOf("</handle>"))
				+" competed in "+txt.substring(txt.lastIndexOf("<short_name>")+12,txt.lastIndexOf("</short_name>"))
				+" ( div-"+roundData.substring(roundData.lastIndexOf("<division>")+10,roundData.lastIndexOf("</division>"))+" )"
					+" on "+txt.substring(txt.lastIndexOf("<date>")+6,txt.lastIndexOf("</date>"))
					+"\n "+(newR>=oldR ? "gained":"lost")+ Math.abs(newR-oldR)+" overall rating points\n\n"
					+"Lvl#1: "+roundData.substring(roundData.lastIndexOf("<level_one_status>")+18,roundData.lastIndexOf("</level_one_status>"))
					+" with "+roundData.substring(roundData.lastIndexOf("<level_one_final_points>")+24,roundData.lastIndexOf("</level_one_final_points>"))
					+"\nLvl#2: "+roundData.substring(roundData.lastIndexOf("<level_two_status>")+18,roundData.lastIndexOf("</level_two_status>"))
					+" with "+roundData.substring(roundData.lastIndexOf("<level_two_final_points>")+24,roundData.lastIndexOf("</level_two_final_points>"))
					+"\nLvl#3: "+roundData.substring(roundData.lastIndexOf("<level_three_status>")+20,roundData.lastIndexOf("</level_three_status>"))
					+" with "+roundData.substring(roundData.lastIndexOf("<level_three_final_points>")+26,roundData.lastIndexOf("</level_three_final_points>"))
					+"\n Total points= "+roundData.substring(roundData.lastIndexOf("<final_points>")+14,roundData.lastIndexOf("</final_points>"))
				;
		
		return res;
	}
	public static String publishParticipating(String access_token,String roundID)
	{
		String txt="";
		try{
			String userinfo=facebook.getUser(access_token);
//			txt=new JSONObject(userinfo).getString("name");
			txt=" @["+new JSONObject(userinfo).getString("id")+"] ";
			String friends[]=facebook.getFriendsID(access_token);
			for(int i=0;i<friends.length;i++)
			{
				if(getCoderIDfromDB(friends[i])!=null)
				{
					if(getCoderLastSRM(friends[i])==roundID)
						txt+=" @["+friends[i]+"]";
				}
			}
			txt+=" are competing this round!";
//			facebook.post(access_token, new JSONObject(userinfo).getString("id"), txt);
			TopCoder.setCoderLastSRM(new JSONObject(userinfo).getString("id"), roundID);
			
			facebook.Action(access_token, "topcoderconnect", "participate", "website", "http://community.topcoder.com/tc?module=MatchDetails&rd="+roundID+"#_#",txt);
		}catch(Exception e){return "TopCoder.publishParticipating:"+e.toString();}
		return txt;
	}
	public static String getSRMResult(String coderID,String roundID)
	{
		String res="",txt="",roundData="",handle="";
		try{
			txt=readFromURL("http://community.topcoder.com/tc?module=BasicData&c=dd_rating_history&cr="+coderID);
			handle=txt.substring(0,txt.indexOf("<round_id>"+roundID+"</round_id>"));
			handle=handle.substring(handle.lastIndexOf("<handle>")+8,handle.lastIndexOf("</handle>"));
			
			txt=txt.substring(txt.lastIndexOf("<round_id>"+roundID+"</round_id>"));
			
			txt=txt.substring(0,txt.indexOf("</row>"));
		}catch(Exception e){return "Error while loading Coder data:<br>"+e.toString();}
		
//		String roundID=txt.substring(txt.lastIndexOf("<round_id>")+10,txt.lastIndexOf("</round_id>"));
		
		try{
			roundData=readFromURL("http://community.topcoder.com/tc?module=BasicData&c=dd_round_results&rd="+roundID);
			roundData=roundData.substring(roundData.indexOf("<coder_id>"+coderID+"</coder_id>"));
			roundData=roundData.substring(0,roundData.indexOf("</row>"));
			
			
			res=handle;
			
			res+=" competed in "+txt.substring(txt.lastIndexOf("<short_name>")+12,txt.lastIndexOf("</short_name>"));
					res+=" ( div"+roundData.substring(roundData.lastIndexOf("<division>")+10,roundData.lastIndexOf("</division>"))+" )";
					int oldR=Integer.parseInt( txt.substring(txt.lastIndexOf("<old_rating>")+12,txt.lastIndexOf("</old_rating>")) );
					int newR=Integer.parseInt( txt.substring(txt.lastIndexOf("<new_rating>")+12,txt.lastIndexOf("</new_rating>")) );
							res+=" on "+txt.substring(txt.lastIndexOf("<date>")+6,txt.lastIndexOf("</date>"));
							res+="\n "+(newR>=oldR ? "gained":"lost")+" "+ Math.abs(newR-oldR)+" overall rating points\n\n";
		}catch(Exception e){return "Error while loading SRM data:1:<br>"+res+"<br>:"+e.toString()+"<br>Coder data:<br>"+txt+"<br>"+"<br>RoundData:<br>"+roundData;}
		try{
			
							res+="Lvl#1: "+roundData.substring(roundData.lastIndexOf("<level_one_status>")+18,roundData.lastIndexOf("</level_one_status>"));
							res+=" with "+roundData.substring(roundData.lastIndexOf("<level_one_final_points>")+24,roundData.lastIndexOf("</level_one_final_points>"));
							
					if(roundData.lastIndexOf("<level_two_status>")!=-1)
					{
						res+="\nLvl#2: "+roundData.substring(roundData.lastIndexOf("<level_two_status>")+18,roundData.lastIndexOf("</level_two_status>"));
						res+=" with "+roundData.substring(roundData.lastIndexOf("<level_two_final_points>")+24,roundData.lastIndexOf("</level_two_final_points>"))+" points";
					}
					if(roundData.lastIndexOf("<level_three_status>")!=-1)
					{
					res+="\nLvl#3: "+roundData.substring(roundData.lastIndexOf("<level_three_status>")+20,roundData.lastIndexOf("</level_three_status>"));
					res+=" with "+roundData.substring(roundData.lastIndexOf("<level_three_final_points>")+26,roundData.lastIndexOf("</level_three_final_points>"))+" points";
					}
					res+="\n\n Final score= "+roundData.substring(roundData.lastIndexOf("<final_points>")+14,roundData.lastIndexOf("</final_points>"));
					
		}catch(Exception e){return "Error while loading SRM data:2:<br>"+res+"<br>:"+e.toString()+"<br>Coder data:<br>"+txt+"<br>"+"<br>RoundData:<br>"+roundData;}
		
		return res;
	}
	
		public static String getTopCoderID(String handle)
		{
			String res="",txt="";
			try{
				URL url = new URL("http://www.topcoder.com/tc?module=SimpleSearch&ha="+handle);
			    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			    InputStream in=connection.getInputStream();
			    int ch;
			    byte buff[]=new byte[in.available()];
	            while((ch=in.read(buff))!=-1)
	            		txt+=new String(buff,0,ch);
			    in.close();
			    String tmp="tc?module=MemberProfile&tab=alg&cr=";
//			    String tmp="tc?module=MemberProfile&cr=";
			    res=txt.substring(txt.indexOf(tmp)+tmp.length(),txt.length());//,res.length());
			    res=res.substring(0,res.indexOf("\""));
			}catch(Exception e){res="Error in getTopCoderID :( \n TopCoder response:\n"+txt+"\nError:\n"+e.getMessage();}
			return res;
		}
		public static String getFacebookIDfromDB(String handle)
		{
			Query q = new Query("topcoder").setFilter(
								new FilterPredicate("handle",
										FilterOperator.EQUAL,
										handle));
			PreparedQuery pq = DatastoreServiceFactory.getDatastoreService().prepare(q);
			return (String)( pq.asList(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(1)).get(0).getProperty("fbid") );
		}
		public static String newTopCoder(String handle,String fb_userinfo)
		{
			String topcoderID=TopCoder.getTopCoderID(handle);
			if(topcoderID.indexOf(":(")!=-1)
				return handle+" doesn't exists :(\n"+topcoderID;

			String facebookID=facebook.extractJSON("id",fb_userinfo);
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

			try{
				com.google.appengine.api.datastore.Transaction txn = datastore.beginTransaction();
				Entity member = new Entity("topcoder",facebookID);
				member.setProperty("topcoderid", topcoderID);
				member.setProperty("topcoderhandle", handle);
				member.setProperty("name", (String
						)new JSONObject(fb_userinfo).get("name"));
				datastore.put(member);
				txn.commit();
			}catch(Exception e){return ":( \n"+e.getMessage();}
			return "OK :)\n";
		}
		public static String postFBWelcome(String handle,String access_token)
		{
			try{
			
				String userinfo=facebook.getUser(access_token);
				String facebookID=facebook.extractJSON("id",userinfo);
				String friends[]=facebook.getFriendsID(access_token);
				
				// no such handle
				String resp=newTopCoder(handle, userinfo);
				if(resp.indexOf(":(")!=-1)
						return resp;
				
				String message="Yush! "+facebook.extractJSON("first_name", userinfo)+" just joined with: \n";
				for(int i=0;i<friends.length;i++)
				{
					Query q = new Query("topcoder").setFilter(
							new FilterPredicate("fbid",
									FilterOperator.EQUAL,
									friends[i]));
					PreparedQuery pq = DatastoreServiceFactory.getDatastoreService().prepare(q);
					if(pq.countEntities(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(1))>0)
						message+= (String)( pq.asList(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(1)).get(0).getProperty("name")+" [ "+(String)( pq.asList(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(1)).get(0).getProperty("topcoderhandle"))+" ]\n" );
				}
//				facebook.post(access_token, facebookID, message);
			}catch(Exception e){return ":( \n"+e.getMessage();}
			return ":)";
		}
		public static String notifySRM(String app_access_token,String roundNumber)
		{
			Query q = new Query("topcoder");
			PreparedQuery pq = DatastoreServiceFactory.getDatastoreService().prepare(q);
			int cnt=pq.countEntities(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(1000));
			List<Entity> res= pq.asList(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(1000));
			String txt="notifying "+cnt+" coders";
			for(int i=0;i<cnt;i++)
			{
				Entity cur=res.get(i);
				
				txt+=((String)cur.getProperty("fbid"))+":<br>"+facebook.postNotification(app_access_token,	((String)cur.getProperty("fbid"))	, ("SRM"+roundNumber+" on doors! Tell friends you're in").replace(" ", "%20"), "?srm"+roundNumber);
			}
			return txt;
		}
		
		
		public static void doGet(HttpServletRequest req, HttpServletResponse resp)
		{
			try{
				if(req.getRequestURI().equals("/oauth/facebook/callback/"))
				{
					String code=req.getParameter("code");					
	//					https://www.facebook.com/dialog/oauth?client_id=453103851408298&redirect_uri=http://yoga1290.appspot.com/oauth/facebook/callback/&scope=user_about_me,publish_stream,friends_about_me&state=tcnotifys560
					if(req.getParameter("state").indexOf("tcnotifys")!=-1)
					{
								String access_token=facebook.getAccessToken("453103851408298","***",code);
								String app_access_token=facebook.getAppAccessToken("453103851408298", "***");
								JSONObject user=new JSONObject(facebook.getUser(access_token));
								resp.getWriter().println("user with ID="+user.get("id"));
								if(user.get("id").equals("870205250")) //IS IT ME?
								{
									resp.getWriter().println("App Access Token="+app_access_token+"<br>");
//									resp.getWriter().println(TopCoder.notifySRM(app_access_token, req.getParameter("state").substring(9)));
									resp.getWriter().println(
											TopCoder.notifySRM(app_access_token, 
															req.getParameter("state").substring(	req.getParameter("state").indexOf("tcnotifys")+9	,	req.getParameter("state").length() ) )
											);
									
								}
					}
					// state=COUNTER_tcnaMESSAGE_href
					else if(req.getParameter("state").indexOf("_tcna")!=-1)
					{
						String access_token=facebook.getAccessToken("453103851408298","***",code);
						String app_access_token=facebook.getAppAccessToken("453103851408298", "***");
						JSONObject user=new JSONObject(facebook.getUser(access_token));
						resp.getWriter().println("user with ID="+user.get("id"));
						
						if(user.get("id").equals("870205250")) //IS IT ME?
						{
								Query q = new Query("topcoder");
								PreparedQuery pq = DatastoreServiceFactory.getDatastoreService().prepare(q);
								int cnt=pq.countEntities(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(1000));
								List<Entity> res= pq.asList(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(1000));
								String txt="notifying "+cnt+" coders";
								
								String COUNTER=req.getParameter("state").substring(0,req.getParameter("state").indexOf("_tcna"));
								String MESSAGE=req.getParameter("state").substring(req.getParameter("state").indexOf("_tcna")+5,req.getParameter("state").indexOf("_href"));
								String HREF=req.getParameter("state").substring(req.getParameter("state").indexOf("_href")+5,req.getParameter("state").length());
								int st=0;
								if(COUNTER.length()>0)
									st=Integer.parseInt(COUNTER);
								for(int i=st;i<Math.min(st+10,cnt);i++)
								{
									Entity cur=res.get(i);
									txt+=((String)cur.getKey().getName())+":<br>"+facebook.postNotification(app_access_token,	((String)cur.getKey().getName())	, (MESSAGE).replace(" ", "%20"), HREF)+"<br>";
								}
								resp.getWriter().println("Notified "+Math.min(cnt, st+10)+" out of "+cnt+"...<br>"+txt);
								resp.getWriter().println("<script>setTimeout(\"top.location.href='https://www.facebook.com/dialog/oauth?client_id=453103851408298&redirect_uri=http://yoga1290.appspot.com/oauth/facebook/callback/&scope=user_about_me,publish_stream,friends_about_me&state="+(st+10)+"_tcna"+MESSAGE+"_href"+HREF+"';\",5000);</script>");
						}
						
					}
					//HOME Page:
					// https://www.facebook.com/dialog/oauth?client_id=453103851408298&redirect_uri=http://yoga1290.appspot.com/oauth/facebook/callback/&scope=user_about_me&state=tcchome
					else if(req.getParameter("state").equals("tcchome")) //TopCoderConnectHome
					{
						String access_token=facebook.getAccessToken("453103851408298","***",code);
						JSONObject user=new JSONObject(facebook.getUser(access_token));
						int coder=-1;
						
						try{
							coder=Integer.parseInt(getCoderIDfromDB((String)user.get("id")));
						}catch(Exception e){coder=-1;}
						
						if(coder==-1)
							resp.getWriter().write("<script>top.location.href=\"https://www.facebook.com/dialog/oauth?client_id=453103851408298&redirect_uri=http://yoga1290.appspot.com/oauth/facebook/callback/&scope=user_about_me,email,publish_stream,friends_about_me,user_education_history&state=\"+prompt('Your TopCoder Handle:')+\"tccnew\";</script>");
						else
							resp.getWriter().write("Thanks "+(String)user.get("name")+"! <br> We'll send you a facebook notification when a new update is here...<a href=\"https://www.facebook.com/pages/TopCoder-Connect-20-Community/446944018699115\">Stay tune on the fb page!</a>");
					}
					else if(req.getParameter("state").indexOf("tccnew")!=-1) //TopCoderConnectCheck
					{
						String access_token=facebook.getAccessToken("453103851408298","***",code);
						resp.getWriter().write("Adding new member <br>"+
							newTopCoder(req.getParameter("state").substring(0,req.getParameter("state").indexOf("tccnew"))
										, facebook.getUser(access_token))
									);
					}
					else if(req.getParameter("state").indexOf("tccpsrm")!=-1) //TopCoderConnectCheck
					{
						String access_token=facebook.getAccessToken("453103851408298","***",code);
						JSONObject user=new JSONObject(facebook.getUser(access_token));
						String txt=TopCoder.getSRMResult(TopCoder.getCoderIDfromDB((String)user.get("id")), 
								req.getParameter("state").substring(7));
						
						resp.getWriter().write("Posting SRM Story: <br>"+txt.replaceAll("\n", "<br>")+"<br><br>Server response:"+
								facebook.post(access_token, (String)user.get("id"),txt)
									);
					}
					// state=tccevent15180 ROUND_ID
					// https://www.facebook.com/dialog/oauth?client_id=453103851408298&redirect_uri=http://yoga1290.appspot.com/oauth/facebook/callback/&scope=user_about_me,publish_stream,friends_about_me&state=tccevent15180
					else if(req.getParameter("state").indexOf("tccevent")!=-1) 
					{
						String access_token=facebook.getAccessToken("453103851408298","***",code);
						
						resp.getWriter().println(
								publishParticipating(access_token, req.getParameter("state").substring(8))+"<br><br>"
							);
					}
					//topcoders here?
					// https://www.facebook.com/dialog/oauth?client_id=453103851408298&redirect_uri=http://yoga1290.appspot.com/oauth/facebook/callback/&scope=user_about_me,email,publish_stream,friends_about_me,user_education_history&state=tcbelbesy yoga1290
//					else if(req.getParameter("state").indexOf("tc")!=-1) //1st callback;callback for Auth code
//					{
//							String access_token=facebook.getAccessToken("453103851408298","***",code);
//							
//							resp.getWriter().println(
//										TopCoder.newTopCoder(req.getParameter("state").substring(2), facebook.getUser(access_token))
////										TopCoder.postFBWelcome(req.getParameter("state").substring(2), access_token)
//									);
//					}
				}
				// community.topcoder.com/tc?module=BasicData&c=dd_round_list
				else if(req.getRequestURI().equals("/topcoder/listsrms"))
				{
					try{
					String txt=readFromURL("http://community.topcoder.com/tc?module=BasicData&c=dd_round_list");
//					while(txt.indexOf("<round_id>")!=-1)
//					{
//						resp.getWriter().write(
//								txt.substring(txt.indexOf("<round_id>")+10,txt.indexOf("</round_id>"))
//								+"\n"+
//								txt.substring(txt.indexOf("<short_name>")+12,txt.indexOf("</short_name>"))
//								);
//						txt=txt.substring(txt.indexOf("</row>"));
//					}
					resp.getWriter().write(txt);
					}catch(Exception e){					resp.getWriter().write(e.toString());}
					
				}
				// /topcoder/srmresult/CODER_ID/ROUND_ID
				else if(req.getRequestURI().indexOf("/topcoder/srmresult/")!=-1)
				{
					String ar[]=req.getRequestURI().split("/");
					resp.getWriter().write(TopCoder.getSRMResult(ar[3],ar[4]).replaceAll("\n", "<br>"));
					//Ask for publish permission
					resp.getWriter().write("<br><a href=\"https://www.facebook.com/dialog/oauth?client_id=453103851408298&redirect_uri=http://yoga1290.appspot.com/oauth/facebook/callback/&scope=user_about_me,publish_stream&state=tccpsrm"+ar[4]+"\">Publish on Wall</a>");
				}
				else if(req.getRequestURI().indexOf("/topcoder/cachesrm/")!=-1)
				{
					String cache=readFromURL("http://community.topcoder.com/tc?module=BasicData&c=dd_round_results&rd="+req.getRequestURI().split("/")[3]);
					resp.getWriter().println(cache);
					
					if(cache.equals("Timeout while fetching URL: http://community.topcoder.com/tc?module=BasicData&c=dd_round_results&rd="+req.getRequestURI().split("/")[3])) return;
					
					Entity tcache=new Entity("topcoder_cache",req.getRequestURI().split("/")[3]);
					tcache.setProperty("data", new com.google.appengine.api.datastore.Text(cache));
					DatastoreServiceFactory.getDatastoreService().put(tcache);
				}
				else if(req.getRequestURI().equals("/topcoder/index.html"))
				{
					resp.getWriter().write("<script>top.location.href=\"https://www.facebook.com/dialog/oauth?client_id=453103851408298&redirect_uri=http://yoga1290.appspot.com/oauth/facebook/callback/&scope=user_about_me&state=tcchome\";</script>");
//						resp.getWriter().write(readFromURL("http://yoga1290.appspot.com/topcoder/index.html"));
				}
				else if(req.getRequestURI().equals("/topcoder"))
				{
//					resp.getWriter().write("<script>top.location.href=\"https://www.facebook.com/dialog/oauth?client_id=453103851408298&redirect_uri=http://yoga1290.appspot.com/oauth/facebook/callback/&scope=user_about_me&state=tcchome\";</script>");
					resp.getWriter().write(readFromURL("http://yoga1290.appspot.com/topcoder/index.html"));
				}
			}catch(Exception e){try{resp.getWriter().write(e.getMessage());}catch(Exception e2){}}
		}
		
		
		
		
		public static void doPost(HttpServletRequest req, HttpServletResponse resp)
		{
			try{
				
				if(req.getRequestURI().equals("/topcoder/notification"))
				{
					if(req.getParameter("par")==null)
					resp.getWriter().write(
					facebook.postNotification(facebook.getAppAccessToken("453103851408298","***"), "870205250", "Testing...", "notification?par=para")
					);
					else
						resp.getWriter().println(req.getRequestURI()+":"+req.getParameter("par"));
				}
				
				// direct?href=URL
				else if(req.getRequestURI().equals("/topcoder/direct"))
				{
					resp.getWriter().write("<script>top.location.href=\"http://"+req.getParameter("href")+"\";</script>");
				}
				else if(req.getRequestURI().equals("/topcoder/cache"))
				{
					Entity tcache=new Entity("topcoder_cache",req.getParameter("key"));
					tcache.setProperty("data", new com.google.appengine.api.datastore.Text(req.getParameter("data")));
					DatastoreServiceFactory.getDatastoreService().put(tcache);
					resp.getWriter().println(":)");
				}
				
			}catch(Exception e){}
		}
}
