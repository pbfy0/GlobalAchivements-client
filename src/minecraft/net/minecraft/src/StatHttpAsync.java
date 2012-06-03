package net.minecraft.src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import argo.jdom.JdomParser;
import argo.jdom.JsonStringNode;

public class StatHttpAsync implements Runnable {
	private String baseUrl = "http://mcachieve.appspot.com/update";
	private static JdomParser JDOM_PARSER = new JdomParser();
	private Session session;
	private String jText;
	private StatFileWriter sfw;
	private Map<StatBase, Integer> stMap;
	StatHttpAsync(StatFileWriter sfw, String text){
		this.session = sfw.session;
		this.jText = text;
		this.stMap = sfw.statMap;
		this.sfw = sfw;
	}
	@Override
	public void run() {
		int respCode = -1;
		try{
			String data = "username=" + URLEncoder.encode(session.username, "UTF-8") + "&sid=" + URLEncoder.encode(session.sessionId, "UTF-8") + "&stats=" + URLEncoder.encode(jText, "UTF-8");
//			System.out.println(data);
			URL url = new URL(baseUrl);
			URLConnection uc = url.openConnection();
			uc.setDoOutput(true);
		    OutputStreamWriter wr = new OutputStreamWriter(uc.getOutputStream());
		    wr.write(data);
		    wr.flush();
		    wr.close();
		    respCode = ((HttpURLConnection)uc).getResponseCode();
		    BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
		    String toparse = "";
		    int ln;
		    while((ln = in.read()) != -1){
		    	toparse += (char)ln;
		    }
//		    System.out.println(toparse);
		    Set<Map.Entry<JsonStringNode, JsonStringNode>> rn = ((Map<JsonStringNode, JsonStringNode>)JDOM_PARSER.parse(toparse).getFields()).entrySet();
			for(Object entryobj : rn){
				Map.Entry<JsonStringNode, JsonStringNode> entry = (Map.Entry<JsonStringNode, JsonStringNode>) entryobj;
				StatBase stid = sfw.getStatById(Integer.parseInt(entry.getKey().getText()));
				if(stid != null){
					stMap.put(stid, Integer.parseInt(entry.getValue().getText()));
				}
	        }
			sfw.func_27179_a(stMap, false);
		}catch(Exception e){
			if(e instanceof UnknownHostException){
				sfw.gaveUp = true;
				return;
			}
			switch(respCode){
				case 401:
					sfw.gaveUp = true;
					break;
				case 500:
					break;
				default:
					e.printStackTrace();
//					System.err.println(e.getMessage());
					break;
			}	
		}
	}
}
