package com.ilin.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundUtil {
  private static SoundUtil mSoundUtil;

  private int soundId;

  public static void init(Context context, int resId) {
    if (null == mSoundUtil) {
      mSoundUtil = new SoundUtil(context, resId);
    }
  }

  public static SoundUtil get() {
    return mSoundUtil;
  }

  private SoundPool soundPool = new SoundPool(6, AudioManager.STREAM_MUSIC, 0);

  private SoundUtil(Context context, int resId) {
    soundId = soundPool.load(context, resId, 1);
  }

  public void play() {
    soundPool.play(soundId, 1, 1, 10, 0, 1);
  }

}
