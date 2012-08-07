package net.minecraft.src;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class StatFileWriter
{
	Session session;
	static Map<StatBase, Integer> statMap;
	static Map<StatBase, Integer> statMapToSend;
	private static Map<Integer, StatBase> reverseMappings;
	GlobalAchievements globalAchievements;

	public StatFileWriter(Session session, File file){
		this(session);
	}
	public StatFileWriter(Session session){
		this.session = session;
		this.statMap = new HashMap<StatBase, Integer>();
		this.statMapToSend = new HashMap<StatBase, Integer>();
		this.reverseMappings = new HashMap<Integer, StatBase>();
		this.globalAchievements = new GlobalAchievements(this);
		for(Object c : StatList.allStats){
			StatBase sb = (StatBase)c;
			reverseMappings.put(sb.statId, sb);
		}
	}
	
	public void readStat(StatBase stat, int value){
		readStat(stat, value, true);
	}
	public void readStat(StatBase stat, int value, boolean tMap){ // actually writes, SUUCH a weird name
		if(stat == null)return;
		Integer cv = statMap.get(stat) == null ? 0 : statMap.get(stat);
		statMap.put(stat, cv + value);
		if(tMap){
			statMapToSend.put(stat,cv + value);
		}
	}
	
	public int writeStat(StatBase stat){ // actually reads it, weird name
		Integer statState = this.statMap.get(stat);
		return statState == null ? 0 : statState;
	}

    public Map func_77445_b() // 77455 is to send, 77457 is perm
    {
        return new HashMap(this.statMapToSend);
    }

    /**
     * write a whole Map of stats to the statmap
     */
    public void writeStats(Map<StatBase, Integer> map){
    	writeStats(map, true);
    }
    public void writeStats(Map<StatBase, Integer> map, boolean tMap) {
    	if (map != null) {
            for(Map.Entry<StatBase, Integer> entry : map.entrySet()){
                this.readStat(entry.getKey(), entry.getValue(),tMap);
            }
        }
    }

/*    public void func_77452_b(Map par1Map)
    {
        if (par1Map != null)
        {
            Iterator var2 = par1Map.keySet().iterator();

            while (var2.hasNext())
            {
                StatBase var3 = (StatBase)var2.next();
                Integer var4 = (Integer)this.field_77455_b.get(var3);
                int var5 = var4 == null ? 0 : var4.intValue();
                this.field_77457_a.put(var3, Integer.valueOf(((Integer)par1Map.get(var3)).intValue() + var5));
            }
        }
    }*/

/*    public void func_77448_c(Map par1Map)
    {
        if (par1Map != null)
        {
            this.field_77456_c = true;
            Iterator var2 = par1Map.keySet().iterator();

            while (var2.hasNext())
            {
                StatBase var3 = (StatBase)var2.next();
                this.writeStatToMap(this.field_77455_b, var3, ((Integer)par1Map.get(var3)).intValue());
            }
        }
    }*/

/*    public static Map func_77453_b(String par0Str)
    {
        HashMap var1 = new HashMap();

        try
        {
            String var2 = "local";
            StringBuilder var3 = new StringBuilder();
            JsonRootNode var4 = (new JdomParser()).parse(par0Str);
            List var5 = var4.getArrayNode(new Object[] {"stats-change"});
            Iterator var6 = var5.iterator();

            while (var6.hasNext())
            {
                JsonNode var7 = (JsonNode)var6.next();
                Map var8 = var7.getFields();
                Entry var9 = (Entry)var8.entrySet().iterator().next();
                int var10 = Integer.parseInt(((JsonStringNode)var9.getKey()).getText());
                int var11 = Integer.parseInt(((JsonNode)var9.getValue()).getText());
                StatBase var12 = StatList.getOneShotStat(var10);

                if (var12 == null)
                {
                    System.out.println(var10 + " is not a valid stat");
                }
                else
                {
                    var3.append(StatList.getOneShotStat(var10).statGuid).append(",");
                    var3.append(var11).append(",");
                    var1.put(var12, Integer.valueOf(var11));
                }
            }

            MD5String var14 = new MD5String(var2);
            String var15 = var14.getMD5String(var3.toString());

            if (!var15.equals(var4.getStringValue(new Object[] {"checksum"})))
            {
                System.out.println("CHECKSUM MISMATCH");
                return null;
            }
        }
        catch (InvalidSyntaxException var13)
        {
            var13.printStackTrace();
        }

        return var1;
    	return null;
    }*/

/*    public static String func_77441_a(String par0Str, String par1Str, Map par2Map)
    {
        StringBuilder var3 = new StringBuilder();
        StringBuilder var4 = new StringBuilder();
        boolean var5 = true;
        var3.append("{\r\n");

        if (par0Str != null && par1Str != null)
        {
            var3.append("  \"user\":{\r\n");
            var3.append("    \"name\":\"").append(par0Str).append("\",\r\n");
            var3.append("    \"sessionid\":\"").append(par1Str).append("\"\r\n");
            var3.append("  },\r\n");
        }

        var3.append("  \"stats-change\":[");
        Iterator var6 = par2Map.keySet().iterator();

        while (var6.hasNext())
        {
            StatBase var7 = (StatBase)var6.next();

            if (var5)
            {
                var5 = false;
            }
            else
            {
                var3.append("},");
            }

            var3.append("\r\n    {\"").append(var7.statId).append("\":").append(par2Map.get(var7));
            var4.append(var7.statGuid).append(",");
            var4.append(par2Map.get(var7)).append(",");
        }

        if (!var5)
        {
            var3.append("}");
        }

        MD5String var8 = new MD5String(par1Str);
        var3.append("\r\n  ],\r\n");
        var3.append("  \"checksum\":\"").append(var8.getMD5String(var4.toString())).append("\"\r\n");
        var3.append("}");
        return var3.toString();
    }*/

    /**
     * Returns true if the achievement has been unlocked.
     */
    public boolean hasAchievementUnlocked(Achievement achievement)
    {
        return this.statMap.containsKey(achievement);
    }

    /**
     * Returns true if the parent has been unlocked, or there is no parent
     */
    public boolean canUnlockAchievement(Achievement achievement)
    {
        return achievement.parentAchievement == null || this.hasAchievementUnlocked(achievement.parentAchievement);
    }

    public void syncStats()
    {
        this.globalAchievements.sync();
    }

    public void func_77449_e()
    {
        /*if (this.field_77456_c && this.statsSyncher.func_77425_c())
        {
            this.statsSyncher.beginSendStats(this.func_77445_b());
        }

        this.statsSyncher.func_77422_e();*/
    }
}
