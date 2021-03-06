package dev.fls.tablist.page.parts.body.lines;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.util.UUIDTypeAdapter;
import dev.fls.tablist.utils.PacketUtils;
import dev.fls.tablist.utils.mojangapi.MinecraftProfile;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BodyLine {

    private LineType lineType;
    private final EntityPlayer entityPlayer;
    private int ping;
    private String text, correctLengthText;
    private final int x,z;

    public BodyLine(String text, int ping, int x, int z) {
        this(text, ping, x, z, x + "." + getZlineCode(z), null);
        lineType = LineType.BLANK;
    }

    public BodyLine(String text, int ping, int x, int z, EntityPlayer entityPlayer) {
        this(text, ping, x, z, x + "." + getZlineCode(z), entityPlayer);
        lineType = LineType.PLAYER;
    }

    public BodyLine(String text, int ping, int x, int z, String name) {
        this(text, ping, x, z, name, null);
        lineType = LineType.BLANK;
    }

    public BodyLine(String text, int x, int z, String name, EntityPlayer entityPlayer) {
        this(text, entityPlayer.ping, x, z, name, entityPlayer);
        lineType = LineType.PLAYER;
    }

    public BodyLine(String text, int ping, int x, int z, String name, EntityPlayer entityPlayer) {
        MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer world = ((CraftWorld) Bukkit.getWorlds().get(0)).getHandle();
        if(entityPlayer != null) {
            this.entityPlayer = entityPlayer;
            this.ping = entityPlayer.ping;
            lineType = LineType.PLAYER;
        } else {
            this.entityPlayer = new EntityPlayer(nmsServer, world, new GameProfile(UUID.randomUUID(), name), new PlayerInteractManager(world));
            this.entityPlayer.ping = this.ping;
            this.ping = ping;
        }
        this.text = text;
        this.correctLengthText = text;
        this.x = x;
        this.z = z;

        setName();
    }

    /**
     * TODO find better method
     * @param z
     * @return
     */
    private static String getZlineCode(int z) {
        String strz = z + "";

        if (z < 10 && z > 0) {
            return 10 + strz;
        } else {
            switch (z) {
                case 10:
                    return 110 + "";
                case 11:
                    return 111 + "";
                default:
                    break;
            }
        }

        return strz;
    }

    private void setName() {
        try {
            Field headerField = entityPlayer.getClass().getDeclaredField("listName");
            headerField.setAccessible(true);
            headerField.set(entityPlayer, IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + correctLengthText + " \"}"));
            headerField.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BodyLine setSkin(String value, String signature) {
        entityPlayer.getProfile().getProperties().removeAll("textures");
        entityPlayer.getProfile().getProperties().put("textures", new Property("textures", value, signature));
        return this;
    }

    public BodyLine setSkin(UUID uuid) {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(String.format("https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false", UUIDTypeAdapter.fromUUID(uuid))).openConnection();
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                Stream<String> reply = new BufferedReader(new InputStreamReader(connection.getInputStream())).lines();
                List<String> lines = reply.collect(Collectors.toList());
                String line = "";
                for(String str : lines) {
                    line += str;
                }
                Gson gson = new Gson();
                MinecraftProfile profile = gson.fromJson(line, MinecraftProfile.class);
                entityPlayer.getProfile().getProperties().put("textures", new Property("textures", profile.getProperties()[0].getValue(), profile.getProperties()[0].getSignature()));
            } else {
                Bukkit.getLogger().severe("BodyLine("+ x +"."+ z + ") skin could not be loaded. Reason: " + connection.getResponseCode() + ", " + connection.getResponseMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public void setPing(int ping) {
        this.ping = ping;
        entityPlayer.ping = this.ping;
    }

    public void setText(String text) {
        if(text.length() > 48) text = text.substring(0,47);
        this.text = text;
        setName();
    }

    public LineType getLineType() {
        return lineType;
    }

    public String getText() {
        return text;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public String getCorrectLengthText() {
        return correctLengthText;
    }

    public void setCorrectLengthText(String txt) {
        this.correctLengthText = txt;
        setName();
    }

    public void show(Player player) {
        Packet[] packets = new Packet[]{
                new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityPlayer),
                new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_LATENCY, entityPlayer),
        };

        for(Packet packet : packets) {
            PacketUtils.sendPacket(player, packet);
        }
    }

    public void updatePing(Player player) {
        Packet[] packets = new Packet[]{
                new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_LATENCY, entityPlayer),
        };

        for(Packet packet : packets) {
            PacketUtils.sendPacket(player, packet);
        }
    }

    public void hide(Player player) {
        Packet[] packets = new Packet[]{
                new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer),
        };

        for(Packet packet : packets) {
            PacketUtils.sendPacket(player, packet);
        }
    }
}
