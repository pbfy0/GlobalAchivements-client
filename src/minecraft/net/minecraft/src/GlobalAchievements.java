package net.minecraft.src;

import static argo.jdom.JsonNodeFactories.aJsonObject;
import static argo.jdom.JsonNodeFactories.aJsonString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import argo.format.CompactJsonFormatter;
import argo.format.JsonFormatter;
import argo.jdom.JdomParser;
import argo.jdom.JsonStringNode;
import net.minecraft.client.Minecraft;

public class GlobalAchievements {
	Minecraft minecraft;
	StatFileWriter statFileWriter;
	private GameSettings gameSettings;
	boolean gaveUp;
	boolean firstRun = true;
	private static JsonFormatter JSON_FORMATTER = new CompactJsonFormatter();
	public static Set<String> modSettings = new HashSet<String>();
	Session session;
	public GlobalAchievements(StatFileWriter statFileWriter) {
		this.statFileWriter = statFileWriter;
		this.session = this.statFileWriter.session;
		this.minecraft = this.getMc();
		this.gameSettings = this.minecraft.gameSettings;
//		System.out.println(this.gameSettings.advancedOpengl);
/*		try{
		System.out.println(StatFileWriter.class.getField("gaveUp").getBoolean(this.statFileWriter));
		}catch(NoSuchFieldException e){
			
			System.out.println("No Such Field");
		}catch(IllegalAccessException e){
			System.out.println("Illegal Access!");
		}*/
	}
	private Minecraft getMc(){
		return this.getMc("P"); // obfuscated name
	}
	private Minecraft getMc(String fn){ // I've being feeling very reflective lately. Yay. getting a private field
		try {
			Field f = Minecraft.class.getDeclaredField(fn);
			f.setAccessible(true);
			return (Minecraft)(f.get(null));
		} catch(NoSuchFieldException e){
			return fn == "theMinecraft" ? null : this.getMc("theMinecraft"); // non-obfuscated name
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
	void sync(){
		syncSettings();
		syncStats();
	}
	private void syncStats(){
		this.gaveUp = this.gaveUp || this.session.sessionId.equals("");
		Map<StatBase, Integer> statMapToSend = this.statFileWriter.statMapToSend;
		if(gaveUp || (!firstRun && statMapToSend.isEmpty())) return;
		firstRun = false;
		Map<JsonStringNode, JsonStringNode> tmap = new HashMap<JsonStringNode, JsonStringNode>();
        for(Map.Entry<StatBase, Integer> entry : statMapToSend.entrySet()){
            tmap.put(aJsonString(entry.getKey().statId + ""), aJsonString(entry.getValue() + ""));
        }
        statMapToSend.clear();
		String text = JSON_FORMATTER.format(aJsonObject(tmap));
		Runnable task = new StatHttpAsync(this, text);
		Thread worker = new Thread(task);
		worker.start();
	}
	private void syncSettings(){
		if(this.gaveUp)return;
		Map<String, String> settings = new HashMap();
		if(!firstRun){
			settings.putAll(this.getVanillaSettings());
			settings.putAll(this.getModSettings());
		}
		Map<JsonStringNode, JsonStringNode> tmap = new HashMap<JsonStringNode, JsonStringNode>();
		for(Map.Entry<String, String> entry : settings.entrySet()){
			tmap.put(aJsonString(entry.getKey()), aJsonString(entry.getValue()));
		}
		String text = JSON_FORMATTER.format(aJsonObject(tmap));
		Runnable task = new SettingHttpAsync(this, text);
		Thread worker = new Thread(task);
		worker.start();
	}
	
	private Map<String, String> getModSettings(){
		Map<String, String> settings = new HashMap<String, String>();
		Class<GameSettings> gsc = GameSettings.class;
		for(String setting : this.modSettings){
			try {
				settings.put(setting, String.valueOf(gsc.getField(setting).get(this.gameSettings)));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return settings;
	}
	void setModSettings(Map<String, String> settings){
		Class<GameSettings> gsc = GameSettings.class;
		for(Map.Entry<String, String> entry : settings.entrySet()){
			if(!(this.modSettings.contains(entry.getKey())))continue;
			try {
				gsc.getField(entry.getKey()).set(this.gameSettings, entry.getValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	static String readString(InputStream is) throws IOException {
		  char[] buf = new char[128];
		  Reader r = new InputStreamReader(is, "UTF-8");
		  StringBuilder s = new StringBuilder(128);
		  while (true) {
		    int n = r.read(buf);
		    if (n < 0)
		      break;
		    String toappend = String.valueOf(buf, 0, n);//new String(buf).substring(0, n);
		    s.append(toappend);
		  }
		  return s.toString();
	}
	private Map<String, String> getVanillaSettings(){
		if(this.minecraft == null)return null;
		GameSettings gameSettings = this.minecraft.gameSettings;
		Map<String, String> settings = new HashMap<String, String>();
		settings.put("musicVolume", String.valueOf(gameSettings.musicVolume));
		settings.put("soundVolume", String.valueOf(gameSettings.soundVolume));
		settings.put("mouseSensitivity", String.valueOf(gameSettings.mouseSensitivity));
		settings.put("invertMouse", String.valueOf(gameSettings.invertMouse));
		settings.put("renderDistance", String.valueOf(gameSettings.renderDistance));
		settings.put("viewBobbing", String.valueOf(gameSettings.viewBobbing));
		settings.put("anaglyph", String.valueOf(gameSettings.anaglyph));
		settings.put("advancedOpengl", String.valueOf(gameSettings.advancedOpengl));
		settings.put("limitFramerate", String.valueOf(gameSettings.limitFramerate));
		settings.put("fancyGraphics", String.valueOf(gameSettings.fancyGraphics));
		settings.put("ambientOcclusion", String.valueOf(gameSettings.ambientOcclusion));
		settings.put("clouds", String.valueOf(gameSettings.clouds));
		settings.put("keyBindForward", String.valueOf(gameSettings.keyBindForward.keyCode));
		settings.put("keyBindLeft", String.valueOf(gameSettings.keyBindLeft.keyCode));
		settings.put("keyBindBack", String.valueOf(gameSettings.keyBindBack.keyCode));
		settings.put("keyBindRight", String.valueOf(gameSettings.keyBindRight.keyCode));
		settings.put("keyBindJump", String.valueOf(gameSettings.keyBindJump.keyCode));
		settings.put("keyBindInventory", String.valueOf(gameSettings.keyBindInventory.keyCode));
		settings.put("keyBindDrop", String.valueOf(gameSettings.keyBindDrop.keyCode));
		settings.put("keyBindChat", String.valueOf(gameSettings.keyBindChat.keyCode));
		settings.put("keyBindSneak", String.valueOf(gameSettings.keyBindSneak.keyCode));
		settings.put("keyBindAttack", String.valueOf(gameSettings.keyBindAttack.keyCode));
		settings.put("keyBindUseItem", String.valueOf(gameSettings.keyBindUseItem.keyCode));
		settings.put("keyBindPlayerList", String.valueOf(gameSettings.keyBindPlayerList.keyCode));
		settings.put("keyBindPickBlock", String.valueOf(gameSettings.keyBindPickBlock.keyCode));
		settings.put("difficulty", String.valueOf(gameSettings.difficulty));
		settings.put("hideGUI", String.valueOf(gameSettings.hideGUI));
		settings.put("thirdPersonView", String.valueOf(gameSettings.thirdPersonView));
		settings.put("showDebugInfo", String.valueOf(gameSettings.showDebugInfo));
		settings.put("lastServer", String.valueOf(gameSettings.lastServer));
		settings.put("noclip", String.valueOf(gameSettings.noclip));
		settings.put("smoothCamera", String.valueOf(gameSettings.smoothCamera));
		settings.put("debugCamEnable", String.valueOf(gameSettings.debugCamEnable));
		settings.put("noclipRate", String.valueOf(gameSettings.noclipRate));
		settings.put("debugCamRate", String.valueOf(gameSettings.debugCamRate));
		settings.put("fovSetting", String.valueOf(gameSettings.fovSetting));
		settings.put("gammaSetting", String.valueOf(gameSettings.gammaSetting));
		settings.put("guiScale", String.valueOf(gameSettings.guiScale));
		settings.put("particleSetting", String.valueOf(gameSettings.particleSetting));
		settings.put("language", String.valueOf(gameSettings.language));
		return settings;
	}
	void setVanillaSettings(Map<String, String> settings){
		this.gameSettings.musicVolume = Float.parseFloat(settings.get("musicVolume"));
		this.gameSettings.soundVolume = Float.parseFloat(settings.get("soundVolume"));
		this.gameSettings.mouseSensitivity = Float.parseFloat(settings.get("mouseSensitivity"));
		this.gameSettings.invertMouse = Boolean.parseBoolean(settings.get("invertMouse"));
		this.gameSettings.renderDistance = Integer.parseInt(settings.get("renderDistance"));
		this.gameSettings.viewBobbing = Boolean.parseBoolean(settings.get("viewBobbing"));
		this.gameSettings.anaglyph = Boolean.parseBoolean(settings.get("anaglyph"));
		this.gameSettings.advancedOpengl = Boolean.parseBoolean(settings.get("advancedOpengl"));
		this.gameSettings.limitFramerate = Integer.parseInt(settings.get("limitFramerate"));
		this.gameSettings.fancyGraphics = Boolean.parseBoolean(settings.get("fancyGraphics"));
		this.gameSettings.ambientOcclusion = Boolean.parseBoolean(settings.get("ambientOcclusion"));
		this.gameSettings.clouds = Boolean.parseBoolean(settings.get("clouds"));
		this.gameSettings.keyBindForward.keyCode = Integer.parseInt(settings.get("keyBindForward"));
		this.gameSettings.keyBindLeft.keyCode = Integer.parseInt(settings.get("keyBindLeft"));
		this.gameSettings.keyBindBack.keyCode = Integer.parseInt(settings.get("keyBindBack"));
		this.gameSettings.keyBindRight.keyCode = Integer.parseInt(settings.get("keyBindRight"));
		this.gameSettings.keyBindJump.keyCode = Integer.parseInt(settings.get("keyBindJump"));
		this.gameSettings.keyBindInventory.keyCode = Integer.parseInt(settings.get("keyBindInventory"));
		this.gameSettings.keyBindDrop.keyCode = Integer.parseInt(settings.get("keyBindDrop"));
		this.gameSettings.keyBindChat.keyCode = Integer.parseInt(settings.get("keyBindChat"));
		this.gameSettings.keyBindSneak.keyCode = Integer.parseInt(settings.get("keyBindSneak"));
		this.gameSettings.keyBindAttack.keyCode = Integer.parseInt(settings.get("keyBindAttack"));
		this.gameSettings.keyBindUseItem.keyCode = Integer.parseInt(settings.get("keyBindUseItem"));
		this.gameSettings.keyBindPlayerList.keyCode = Integer.parseInt(settings.get("keyBindPlayerList"));
		this.gameSettings.keyBindPickBlock.keyCode = Integer.parseInt(settings.get("keyBindPickBlock"));
		this.gameSettings.difficulty = Integer.parseInt(settings.get("difficulty"));
		this.gameSettings.hideGUI = Boolean.parseBoolean(settings.get("hideGUI"));
		this.gameSettings.thirdPersonView = Integer.parseInt(settings.get("thirdPersonView"));
		this.gameSettings.showDebugInfo = Boolean.parseBoolean(settings.get("showDebugInfo"));
		this.gameSettings.lastServer = settings.get("lastServer");
		this.gameSettings.noclip = Boolean.parseBoolean(settings.get("noclip"));
		this.gameSettings.smoothCamera = Boolean.parseBoolean(settings.get("smoothCamera"));
		this.gameSettings.debugCamEnable = Boolean.parseBoolean(settings.get("debugCamEnable"));
		this.gameSettings.noclipRate = Float.parseFloat(settings.get("noclipRate"));
		this.gameSettings.debugCamRate = Float.parseFloat(settings.get("debugCamRate"));
		this.gameSettings.fovSetting = Float.parseFloat(settings.get("fovSetting"));
		this.gameSettings.gammaSetting = Float.parseFloat(settings.get("gammaSetting"));
		this.gameSettings.guiScale = Integer.parseInt(settings.get("guiScale"));
		this.gameSettings.particleSetting = Integer.parseInt(settings.get("particleSetting"));
		this.gameSettings.language = settings.get("language");
	}
}

abstract class HttpAsync implements Runnable {
//	private String baseUrl = ""; 
	private static JdomParser JDOM_PARSER = new JdomParser();
	GlobalAchievements ga;
	private String jsonText;
	HttpAsync(GlobalAchievements ga, String jsonText){
		this.ga = ga;
		this.jsonText = jsonText;
	}
	abstract String getBaseUrl();
	@Override
	public void run() {
		int respCode = -1;
		try{
			String data = "username=" + URLEncoder.encode(this.ga.session.username, "UTF-8") + "&sid=" + URLEncoder.encode(this.ga.session.sessionId, "UTF-8") + "&stats=" + URLEncoder.encode(this.jsonText, "UTF-8");
//			System.out.println(getBaseUrl());
			URL url = new URL(getBaseUrl());
			URLConnection uc = url.openConnection();
			uc.setDoOutput(true);
		    OutputStreamWriter wr = new OutputStreamWriter(uc.getOutputStream());
		    wr.write(data);
		    wr.flush();
		    wr.close();
		    respCode = ((HttpURLConnection)uc).getResponseCode();
		    String toparse = GlobalAchievements.readString(uc.getInputStream());
//		    System.out.println("\"" + toparse + "\"");
		    Set<Map.Entry<JsonStringNode, JsonStringNode>> jsno = ((Map<JsonStringNode, JsonStringNode>)JDOM_PARSER.parse(toparse).getFields()).entrySet();
		    Map<String, String> omap = new HashMap<String, String>();
		    for(Map.Entry<JsonStringNode, JsonStringNode> c : jsno){
		    	omap.put(c.getKey().getText(), c.getValue().getText());
		    }
		    this.handle(omap);
		}catch(UnknownHostException e){
			this.ga.gaveUp = true; // no network
			return;
		}catch(IOException e){
			switch(respCode){
				case 401:
					this.ga.gaveUp = true; // bad login
					break;
				case 500:
					break; // something else server side
			}	
		}catch(Exception e){
			e.printStackTrace(); // something else
//			System.err.println(e.getMessage());
		}
	}
	abstract void handle(Map<String, String> parsed);
}

class StatHttpAsync extends HttpAsync {
//	private String baseUrl = "http://mcachieve.appspot.com/stat";
	StatHttpAsync(GlobalAchievements ga, String jsonText) {
		super(ga, jsonText);
	}
	
	String getBaseUrl(){
		return "http://mcachieve.appspot.com/stat";
	}
	
	@Override
	void handle(Map<String, String> parsed){
		Map<StatBase, Integer> statMap = new HashMap<StatBase, Integer>();
		for(Map.Entry<String, String> entry : parsed.entrySet()){
			StatBase stat = StatList.getOneShotStat(Integer.parseInt(entry.getKey()));
			if(stat != null){
				statMap.put(stat, Integer.parseInt(entry.getValue()));
			}
		}
		this.ga.statFileWriter.writeStats(statMap, false);
	}
}
class SettingHttpAsync extends HttpAsync {
	SettingHttpAsync(GlobalAchievements ga, String jsonText) {
		super(ga, jsonText);
	}

	String getBaseUrl(){
		return "http://mcachieve.appspot.com/setting";
	}
	
	@Override
	void handle(Map<String, String> parsed) {
		this.ga.setVanillaSettings(parsed);
		this.ga.setModSettings(parsed);
	}
}