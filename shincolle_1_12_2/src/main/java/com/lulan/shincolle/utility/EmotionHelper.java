package com.lulan.shincolle.utility;

import com.lulan.shincolle.client.model.IModelEmotion;
import com.lulan.shincolle.client.model.IModelEmotionAdv;
import com.lulan.shincolle.entity.IShipEmotion;
import com.lulan.shincolle.reference.Values;
import net.minecraft.util.math.MathHelper;

public class EmotionHelper {
    private EmotionHelper() {}

    public static void rollEmotion(IModelEmotion model, IShipEmotion ent) {
        switch (ent.getStateEmotion(1)) {
            case 6: 
            case 7: 
            case 8: 
            case 9: {
                model.setFace(ent.getStateEmotion(1) - 5);
                break;
            }
            case 1: {
                EmotionHelper.applyEmotionBlink(model, ent);
                break;
            }
            case 2: {
                model.setFace(2);
                break;
            }
            case 3: {
                EmotionHelper.applyEmotion(model, ent, 3, 45);
                break;
            }
            case 5: {
                model.setFace(4);
                break;
            }
            default: {
                if (ent.getFaceTick() <= 0) {
                    model.setFace(0);
                } else {
                    EmotionHelper.applyEmotionBlink(model, ent);
                }
                if ((ent.getTickExisted() & 0x7F) != 0 || (ent.getRand().nextInt(10)) <= 7) break;
                EmotionHelper.applyEmotionBlink(model, ent);
            }
        }
    }

    public static void rollEmotionAdv(IModelEmotionAdv model, IShipEmotion ent) {
        switch (ent.getStateEmotion(1)) {
            case 9: {
                model.setFace(ent.getStateEmotion(1));
                break;
            }
            case 1: {
                EmotionHelper.applyEmotionBlinkAdv(model, ent);
                break;
            }
            case 2: {
                EmotionHelper.applyEmotionAdv(model, ent, 2, 80);
                break;
            }
            case 3: {
                EmotionHelper.applyEmotionAdv(model, ent, 3, 45);
                break;
            }
            case 5: {
                model.setFaceHungry(ent);
                ent.setFaceTick(-1);
                break;
            }
            case 6: {
                EmotionHelper.applyEmotionAdv(model, ent, 6, 40);
                break;
            }
            case 7: {
                EmotionHelper.applyEmotionAdv(model, ent, 7, 80);
                break;
            }
            case 8: {
                EmotionHelper.applyEmotionAdv(model, ent, 8, 60);
                break;
            }
            case 4: {
                model.setFaceBored(ent);
                break;
            }
            default: {
                if (ent.getFaceTick() <= 0) {
                    if (ent.getStateEmotion(1) != 4) {
                        model.setFaceNormal(ent);
                    }
                } else {
                    EmotionHelper.applyEmotionBlinkAdv(model, ent);
                }
                if ((ent.getTickExisted() & 0x7F) != 0 || (ent.getRand().nextInt(10)) <= 7) break;
                EmotionHelper.applyEmotionBlinkAdv(model, ent);
            }
        }
    }

    public static float getHeadTiltAngle(IShipEmotion ent, float f2) {
        int cd = ent.getTickExisted() - ent.getHeadTiltTick();
        float maxAngle = -0.27f;
        float partTick = f2 - ((int)f2) + cd;
        if (cd > 70 + ent.getRand().nextInt(5)) {
            ent.setHeadTiltTick(ent.getTickExisted());
            partTick = f2 - ((int)f2);
            ent.setStateFlag(8, ent.getRand().nextInt(10) > 4);
        }
        if (ent.getStateFlag(8)) {
            if (ent.getStateEmotion(2) > 0) {
                return maxAngle;
            }
            float f = MathHelper.sin(partTick * 0.1f * 1.5708f) * maxAngle;
            if (f - 0.03f < maxAngle || partTick > 10.0f) {
                ent.setStateEmotion(2, 1, false);
                f = maxAngle;
            }
            return f;
        }
        if (ent.getStateEmotion(2) <= 0) {
            return 0.0f;
        }
        float f = (1.0f - MathHelper.sin(partTick * 0.2f * 1.5708f)) * maxAngle;
        if (f + 0.03f > 0.0f || partTick > 8.0f) {
            ent.setStateEmotion(2, 0, false);
            f = 0.0f;
        }
        return f;
    }

    public static void applyEmotionBlink(IModelEmotion model, IShipEmotion ent) {
        int EmoTime;
        if (ent.getStateEmotion(1) == 0) {
            ent.setFaceTick(ent.getTickExisted());
            ent.setStateEmotion(1, 1, false);
            model.setFace(1);
        }
        if ((EmoTime = ent.getTickExisted() - ent.getFaceTick()) > 25) {
            model.setFace(0);
            if (ent.getStateEmotion(1) == 1) {
                ent.setStateEmotion(1, 0, false);
            }
            ent.setFaceTick(-1);
        } else if (EmoTime > 20) {
            model.setFace(1);
        } else if (EmoTime > 10) {
            model.setFace(0);
        } else if (EmoTime > -1) {
            model.setFace(1);
        }
    }

    public static void applyEmotionBlinkAdv(IModelEmotionAdv model, IShipEmotion ent) {
        int emoTime;
        if (ent.getStateEmotion(1) == 0) {
            ent.setFaceTick(ent.getTickExisted());
            ent.setStateEmotion(1, 1, false);
            model.setFaceBlink1(ent);
        }
        if ((emoTime = ent.getTickExisted() - ent.getFaceTick()) > 25) {
            model.setFaceBlink0(ent);
            if (ent.getStateEmotion(1) == 1) {
                ent.setStateEmotion(1, 0, false);
            }
            ent.setFaceTick(-1);
        } else if (emoTime > 20) {
            model.setFaceBlink1(ent);
        } else if (emoTime > 10) {
            model.setFaceBlink0(ent);
        } else if (emoTime > -1) {
            model.setFaceBlink1(ent);
        }
    }

    public static void applyEmotion(IModelEmotion model, IShipEmotion ent, int type, int maxTime) {
        if (ent.getFaceTick() <= 0) {
            ent.setFaceTick(ent.getTickExisted());
        }
        if ((ent.getTickExisted() - ent.getFaceTick()) > maxTime) {
            model.setFace(0);
            ent.setStateEmotion(1, 0, false);
            ent.setFaceTick(-1);
        } else {
            if(type == 3){
                model.setFace(3);
            }
        }
    }

    public static void applyEmotionAdv(IModelEmotionAdv model, IShipEmotion ent, int type, int maxTime) {
        if (ent.getFaceTick() <= 0) {
            ent.setFaceTick(ent.getTickExisted());
        }
        if ((ent.getTickExisted() - ent.getFaceTick()) > maxTime) {
            model.setFaceNormal(ent);
            ent.setStateEmotion(1, 0, false);
            ent.setFaceTick(-1);
        } else {
            switch (type) {
                case 3: {
                    if ((ent.getTickExisted() & 0x7FF) > 1024) {
                        model.setFaceDamaged(ent);
                        break;
                    }
                    model.setFaceScorn(ent);
                    break;
                }
                case 2: {
                    model.setFaceCry(ent);
                    break;
                }
                case 6: {
                    model.setFaceAngry(ent);
                    break;
                }
                case 7: {
                    model.setFaceShy(ent);
                    break;
                }
                case 8: {
                    model.setFaceHappy(ent);
                    break;
                }
                default:
            }
        }
    }

    public static boolean checkModelState(int id, int state) {
        return (state & Values.N.Pow2[id]) == Values.N.Pow2[id];
    }
}
