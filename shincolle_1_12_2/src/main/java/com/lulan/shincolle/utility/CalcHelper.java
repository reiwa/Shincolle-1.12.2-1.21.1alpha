package com.lulan.shincolle.utility;

import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

public class CalcHelper {
    protected static float[] NORM_TABLE = new float[2000];
    private static final float NORM_MIN = 0.2f;

    public static String tick2SecOrMin(int ticks) {
        int t = (int)(ticks * 0.05f);
        if (t >= 60) {
            t = (int)(t * 0.016666668f);
            return t + "m";
        }
        return t + "s";
    }

    public static String getTimeFormated(int sec) {
        int hours = sec / 3600;
        int minutes = (sec % 3600) / 60;
        int seconds = sec % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static int min(double a, double b, double c) {
        if (a <= b) {
            if (a <= c) {
                return 1;
            }
            return 3;
        }
        if (b <= c) {
            return 2;
        }
        return 3;
    }

    public static float calcNormalDist(float x, float mean, float sd) {
        float s1 = 2.5066283f;
        float s2 = 1.0f / (sd * s1);
        float s3 = x - mean;
        float s4 = -(s3 * s3);
        float s5 = 2.0f * sd * sd;
        float s6 = (float)Math.exp(s4 / s5);
        return s2 * s6;
    }

    public static float getNormDist(int x) {
        if (x > -1 && x < 2000) {
            return NORM_TABLE[x];
        }
        return NORM_MIN;
    }

    public static String[] stringConvNewlineToArray(String str) {
        return str.split("<BR>|<BR/>|<br>|<br/>");
    }

    public static List<String> stringConvNewlineToList(String str) {
        return new ArrayList<>(Arrays.asList(CalcHelper.stringConvNewlineToArray(str)));
    }

    public static int[] intSetToArray(Set<Integer> iset) {
        if (iset != null && !iset.isEmpty()) {
            int[] iarray = new int[iset.size()];
            int id = 0;
            Iterator<Integer> iterator = iset.iterator();
            while (iterator.hasNext()) {
                iarray[id] = iterator.next().intValue();
                ++id;
            }
            return iarray;
        }
        return new int[0];
    }

    public static List<Integer> intArrayToList(int[] iarray) {
        if (iarray != null && iarray.length > 0) {
            ArrayList<Integer> ilist = new ArrayList<>();
            for (int i = 0; i < iarray.length; ++i) {
                ilist.add(iarray[i]);
            }
            return ilist;
        }
        return new ArrayList<>();
    }

    public static int[] intListToArray(List<Integer> ilist) {
        if (ilist != null && !ilist.isEmpty()) {
            int[] iarray = new int[ilist.size()];
            for (int i = 0; i < ilist.size(); ++i) {
                iarray[i] = ilist.get(i);
            }
            return iarray;
        }
        return new int[0];
    }

    public static List listUnion(List list1, List list2) {
        HashSet set1 = new HashSet();
        set1.addAll(list1);
        set1.addAll(list2);
        return new ArrayList(set1);
    }

    public static boolean isAbsGreater(double a, double b) {
        return Math.abs(a) > Math.abs(b);
    }

    public static float[] getLookDegree(double motX, double motY, double motZ, boolean getDegree) {
        double d1 = MathHelper.sqrt(motX * motX + motY * motY + motZ * motZ);
        if (d1 > 1.0E-4) {
            motX /= d1;
            motY /= d1;
            motZ /= d1;
        }
        double f1 = MathHelper.sqrt(motX * motX + motZ * motZ);
        float[] degree = new float[2];
        degree[1] = -((float)Math.atan2(motY, f1));
        degree[0] = -((float)Math.atan2(motX, motZ));
        if (getDegree) {
            degree[0] = degree[0] * 57.29578f;
            degree[1] = degree[1] * 57.29578f;
        }
        return degree;
    }

    @SideOnly(value=Side.CLIENT)
    public static int getEntityHitHeightByClientPlayer(Entity target) {
        return CalcHelper.getEntityHitHeight(ClientProxy.getClientPlayer(), target);
    }

    public static int getEntityHitHeight(Entity host, Entity target) {
        int result = 0;
        if (target != null && host != null && target.height > 0.1f) {
            float x1 = (float)Math.tan(host.rotationPitch * ((float)Math.PI / 180));
            if (x1 > 30.0f) {
                return -10;
            }
            if (x1 < -30.0f) {
                return 110;
            }
            float dist = host.getDistance(target) - target.width * 0.5f;
            if (dist < 0.0f) {
                dist = 0.0f;
            }
            float x2 = (float)(host.posY + host.getEyeHeight() - target.posY);
            float x = x2 - (x1 *= dist);
            result = (int)(x / target.height * 100.0f);
        }
        return result;
    }

    @SideOnly(value=Side.CLIENT)
    public static int getEntityHitSideByClientPlayer(Entity target) {
        int result = 0;
        if (target != null) {
            float angHost = ClientProxy.getClientPlayer().rotationYawHead;
            float angTarget = 0.0f;
            angTarget = target instanceof EntityLivingBase ? ((EntityLivingBase)target).renderYawOffset : target.rotationYaw;
            result = (int)((angHost % 360.0f - angTarget % 360.0f) % 360.0f);
            if (result < 0) {
                result += 360;
            }
        }
        return result;
    }

    public static int getEntityHitSide(Entity host, Entity target) {
        int result = 0;
        if (host != null && target != null && (result = (int)((host.rotationYaw % 360.0f - target.rotationYaw % 360.0f) % 360.0f)) < 0) {
            result += 360;
        }
        return result;
    }

    public static double[] rotateParticleByFace(double x, double y, double z, int f, int len) {
        double[] newParm = new double[3];
        newParm[1] = y;
        switch (f) {
            case 5: {
                newParm[0] = len - z;
                newParm[2] = x;
                break;
            }
            case 3: {
                newParm[0] = len - x;
                newParm[2] = len - z;
                break;
            }
            case 4: {
                newParm[0] = z;
                newParm[2] = len - x;
                break;
            }
            default: {
                newParm[0] = x;
                newParm[2] = z;
            }
        }
        return newParm;
    }

    public static float[] rotateXYZByYawPitch(float x, float y, float z, float yaw, float pitch, float scale) {
        float cosYaw = MathHelper.cos(yaw);
        float sinYaw = MathHelper.sin(yaw);
        float cosPitch = MathHelper.cos(-pitch);
        float sinPitch = MathHelper.sin(-pitch);
        float[] newPos = new float[]{x, y, z};
        newPos[1] = y * cosPitch + z * sinPitch;
        newPos[2] = z * cosPitch - y * sinPitch;
        float x2 = newPos[0];
        float z2 = newPos[2];
        newPos[0] = x2 * cosYaw - z2 * sinYaw;
        newPos[2] = z2 * cosYaw + x2 * sinYaw;
        newPos[0] = newPos[0] * scale;
        newPos[1] = newPos[1] * scale;
        newPos[2] = newPos[2] * scale;
        return newPos;
    }

    public static float[] rotateXZByAxis(float z, float x, float rad, float scale) {
        float cosD = MathHelper.cos(rad);
        float sinD = MathHelper.sin(rad);
        float[] newPos = new float[]{0.0f, 0.0f};
        newPos[0] = z * cosD + x * sinD;
        newPos[1] = x * cosD - z * sinD;
        newPos[0] = newPos[0] * scale;
        newPos[1] = newPos[1] * scale;
        return newPos;
    }

    public static boolean checkIntNotInArray(int target, int[] host) {
        if (host == null) {
            return true;
        }
        for (int i : host) {
            if (target != i) continue;
            return false;
        }
        return true;
    }

    public static Dist4d getDistanceFromA2B(BlockPos host, BlockPos target) {
        if (host != null && target != null) {
            return CalcHelper.getDistanceFromA2B(new Vec3d(host.getX(), host.getY(), host.getZ()), new Vec3d(target.getX(), target.getY(), target.getZ()));
        }
        return Dist4d.ZERO;
    }

    public static Dist4d getDistanceFromA2B(Entity host, Entity target) {
        if (host != null && target != null) {
            return CalcHelper.getDistanceFromA2B(host.getPositionVector(), target.getPositionVector());
        }
        return Dist4d.ZERO;
    }

    public static Dist4d getDistanceFromA2B(Vec3d host, Vec3d target) {
        double z;
        double y;
        double x;
        double dist;
        if (host != null && target != null && (dist = MathHelper.sqrt((x = target.x - host.x) * x + (y = target.y - host.y) * y + (z = target.z - host.z) * z)) > 1.0E-4) {
            return new Dist4d(x /= dist, y /= dist, z /= dist, dist);
        }
        return Dist4d.ZERO;
    }

    public static Vec3d getUnitVectorFromA2B(Vec3d from, Vec3d to) {
        double z;
        double y;
        double x;
        double dist;
        if (from != null && to != null && (dist = MathHelper.sqrt((x = to.x - from.x) * x + (y = to.y - from.y) * y + (z = to.z - from.z) * z)) > 1.0E-4) {
            return new Vec3d(x /= dist, y /= dist, z /= dist);
        }
        return Vec3d.ZERO;
    }

    static {
        for (int i = 0; i < 2000; ++i) {
            CalcHelper.NORM_TABLE[i] = CalcHelper.calcNormalDist(0.5f - i * 2.5E-4f, 0.5f, 0.2f) * 0.50132567f;
            if (NORM_TABLE[i] >= NORM_MIN) continue;
            CalcHelper.NORM_TABLE[i] = NORM_MIN;
        }
    }
}