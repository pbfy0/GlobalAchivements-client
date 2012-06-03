package net.minecraft.src;

import argo.format.CompactJsonFormatter;
import argo.format.JsonFormatter;
import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.jdom.JsonObjectNodeBuilder;
import argo.jdom.JsonRootNode;
import argo.jdom.JsonStringNode;
import argo.saj.InvalidSyntaxException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static argo.jdom.JsonNodeFactories.*;

// GlobalAchivements mod main code

public class StatFileWriter
{
	Session session;
	static Map<StatBase, Integer> statMap;
	static Map<StatBase, Integer> statMapToSend;
	private static Map<Integer, StatBase> reverseMappings;
	private static JsonFormatter JSON_FORMATTER;
	boolean gaveUp = false;
	private boolean firstRun = true;
//	private static int tid = 0;
	public StatFileWriter(Session session, File file){
		this.session = session;
		statMap = new HashMap<StatBase, Integer>();
		statMapToSend = new HashMap<StatBase, Integer>();
		JSON_FORMATTER = new CompactJsonFormatter();
		reverseMappings = new HashMap<Integer, StatBase>();
		for(Object c : StatList.allStats){
			StatBase sb = (StatBase)c;
			reverseMappings.put(sb.statId, sb);
		}
	}
	public void readStat(StatBase stat, int value){
		readStat(stat, value, true);
	}
	public void readStat(StatBase stat, int value, boolean TMap){ // actually writes, SUUCH a weird name
		if(stat == null)return;
		Integer cv = statMap.get(stat) == null ? 0 : statMap.get(stat);
		statMap.put(stat, cv + value);
		if(TMap){
			statMapToSend.put(stat,cv + value);
		}
	}
	public int writeStat(StatBase stat){ // actually reads it, weird name
		Integer statState = statMap.get(stat);
		return statState == null ? 0 : statState;
	}
    public void func_27175_b() {
    	// this space intentionally left blank
    }
    
    public static Map func_27177_a(String qqq){
    	return new HashMap(); // idk what this does, but it seems to work as-is
    }
    
    public void func_27179_a(Map<StatBase, Integer> par1Map) {
    	func_27179_a(par1Map, true);
    }

    public void func_27179_a(Map<StatBase, Integer> par1Map, boolean TMap) {
        if (par1Map != null) {
            for(Map.Entry<StatBase, Integer> entry : par1Map.entrySet()){
                readStat(entry.getKey(), entry.getValue(), TMap);
            }
        }
    }
	public static String func_27185_a(String username, String string, Map par1Map) {
		return "";
	}
	public void func_27187_c(Map field_27436_c) {
		// afaik this doesn't need anything
	}
	public void func_27180_b(Map field_27437_b) {
		// afaik this doesn't need anything
	}
	public void syncStats() {
/*		StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
		String cn = caller.getClassName();
		String mn = caller.getMethodName();
		//System.out.println(cn + " " + mn);
		if(cn == "net.minecraft.client.Minecraft" && (mn == "displayGuiScreen" || mn == "a")){ // hacky solution, but........
			System.err.println("Skipped");
			return;
		}*///accursed obfuscator,how I hate thee, breaking my beautiful (not really) reflection code!
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
//		worker.setName(tid + "");
//		System.out.println("Starting thread " + tid);
//		tid++;
		worker.start();
//		System.out.println(text);
	}
	StatBase getStatById(Integer id){
		return reverseMappings.get(id);
	}
	public void func_27178_d() {
		// afaik this doesn't need anything
	}
	public boolean hasAchievementUnlocked(Achievement ach) {
		return statMap.containsKey(ach);
	}
	public boolean canUnlockAchievement(Achievement ach) {
		return ach.parentAchievement == null ? true : hasAchievementUnlocked(ach.parentAchievement);
	}
}
