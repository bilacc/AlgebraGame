package hr.algebra.algebragame.views;

import android.animation.Animator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import hr.algebra.algebragame.R;

public class SpotOnView extends View {

    private static final String HIGH_SCORE = "HIGH_SCORE";
    private static final int INITIAL_ANIMATION_DURATION = 6000;
    private static final Random random = new Random();
    private static final float SCALE_X = 0.25F;
    private static final float SCALE_Y = 0.25F;
    private static final int INITIAL_SPOTS = 5;
    private static final int SPOT_DELAY = 500;
    private static final int LIVES = 3;
    private static final int MAX_LIVES = 7;
    private static final int NEW_LEVEL = 10;
    private static final int HIT_SOUND_ID = 1;
    private static final int MISS_SOUND_ID = 2;
    private static final int DISAPPEAR_SOUND_ID = 3;
    private static final int SOUND_PRIORITY = 1;
    private static final int SOUND_QUALITY = 100;
    private static final int MAX_STREAMS = 4;
    private static int SPOT_DIAMETER = 160;
    private final Queue<ImageView> spots = new ConcurrentLinkedQueue<>();
    private final Queue<Animator> animators = new ConcurrentLinkedQueue<>();
    private SharedPreferences preferences;
    private int spotsTouched;
    private int score;
    private int level;
    private int viewWidt;
    private int viewHeight;
    private long animationTime;
    private boolean gameOver;
    private boolean gamePaused;
    private boolean dialogDisplayed;
    private int highScore;
    private TextView highScoreTextView;
    private TextView currentScoreTextView;
    private TextView levelTextView;
    private LinearLayout livesLinearLayout;
    private RelativeLayout relativeLayout;
    private Resources resources;
    private LayoutInflater layoutInflater;
    private Handler spotHandler;
    private SoundPool soundPool;
    private int volume;
    private Map<Integer, Integer> soundMap;
    private Runnable addSpotRunnable = new Runnable() {
        @Override
        public void run() {
            addNewSpot();
        }
    };

    public SpotOnView(Context context, SharedPreferences sharedPreferences,
            RelativeLayout parentLayout) {
        super(context);
        preferences = sharedPreferences;
        highScore = preferences.getInt(HIGH_SCORE, 0);
        resources = context.getResources();
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        relativeLayout = parentLayout;
        livesLinearLayout = (LinearLayout) relativeLayout.findViewById(R.id.lifeLinearLayout);
        highScoreTextView = (TextView) relativeLayout.findViewById(R.id.highScoreTextView);
        currentScoreTextView = (TextView) relativeLayout.findViewById(R.id.scoreTextView);
        levelTextView = (TextView) relativeLayout.findViewById(R.id.levelTextView);
        spotHandler = new Handler();
        setSpotDiameter();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        viewWidt = w;
        viewHeight = h;
    }

    public void pause() {
        gamePaused = true;
        soundPool.release();
        soundPool = null;
        cancelAnimations();
    }

    public void resume(Context context) {
        gamePaused = false;
        initializeSoundEffects(context);
        if (!dialogDisplayed) {
            resetGame();
        }
    }

    private void cancelAnimations() {
        for (Animator animator : animators) {
            animator.cancel();
        }
        for (ImageView imageView : spots) {
            relativeLayout.removeView(imageView);
        }
        spotHandler.removeCallbacks(addSpotRunnable);
        animators.clear();
        spots.clear();
    }

    public void resetGame() {
        spots.clear();
        animators.clear();
        livesLinearLayout.removeAllViews();
        animationTime = INITIAL_ANIMATION_DURATION;
        spotsTouched = 0;
        score = 0;
        level = 1;
        gameOver = false;
        displayScores();
        for (int i = 0; i < LIVES; i++) {
            livesLinearLayout.addView(layoutInflater.inflate(R.layout.layout_life, null));
        }
        for (int i = 1; i <= INITIAL_SPOTS; ++i) {
            spotHandler.postDelayed(addSpotRunnable, i * SPOT_DELAY);
        }
    }

    private void initializeSoundEffects(Context context) {
        soundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, SOUND_QUALITY);
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        soundMap = new HashMap<Integer, Integer>();
        soundMap.put(HIT_SOUND_ID, soundPool.load(context, R.raw.hit, SOUND_PRIORITY));
        soundMap.put(MISS_SOUND_ID, soundPool.load(context, R.raw.miss, SOUND_PRIORITY));
        soundMap.put(DISAPPEAR_SOUND_ID, soundPool.load(context, R.raw.disappear, SOUND_PRIORITY));
    }

    private void displayScores() {
        highScoreTextView.setText(resources.getString(R.string.high_score) + " " + highScore);
        currentScoreTextView.setText(resources.getString(R.string.score) + " " + score);
        levelTextView.setText(resources.getString(R.string.level) + " " + level);
    }

    public void addNewSpot() {
        int x = random.nextInt(viewWidt - SPOT_DIAMETER);
        int y = random.nextInt(viewHeight - SPOT_DIAMETER);
        int x2 = random.nextInt(viewWidt - SPOT_DIAMETER);
        int y2 = random.nextInt(viewHeight - SPOT_DIAMETER);
        final ImageView spot = (ImageView) layoutInflater.inflate(R.layout.layout_untouched, null);
        spots.add(spot);
        spot.setLayoutParams(new RelativeLayout.LayoutParams(SPOT_DIAMETER, SPOT_DIAMETER));
        spot.setImageResource(random.nextInt(2) == 0 ? R.drawable.green_spot : R.drawable.red_spot);
        spot.setX(x);
        spot.setY(y);
        spot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                touchedSpot(spot);
            }
        });
        relativeLayout.addView(spot);
        spot.animate().x(x2).y(y2).scaleX(SCALE_X).scaleY(SCALE_Y)
                .setDuration(animationTime).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                animators.add(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animators.remove(animation);
                if (!gamePaused && spots.contains(spot)) {
                    missedSpot(spot);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (soundPool != null) {
            soundPool.play(MISS_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1F);
        }
        decreaseScore();
        score = Math.max(score, 0);
        displayScores();
        return true;
    }

    private void touchedSpot(ImageView spot) {
        relativeLayout.removeView(spot);
        spots.remove(spot);
        ++spotsTouched;
        increaseScore();
        if (soundPool != null) {
            soundPool.play(HIT_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1F);
        }
        if (spotsTouched % NEW_LEVEL == 0) {
            ++level;
            animationTime *= 0.95;
            if (livesLinearLayout.getChildCount() < MAX_LIVES) {
                LinearLayout life = (LinearLayout) layoutInflater.inflate(R.layout.layout_life, null);
                livesLinearLayout.addView(life);
            }
        }
        displayScores();
        if (!gameOver) {
            addNewSpot();
        }
    }

    public void missedSpot(ImageView spot) {
        spots.remove(spot);
        relativeLayout.removeView(spot);
        if (gameOver) {
            return;
        }
        if (soundPool != null) {
            soundPool.play(DISAPPEAR_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1F);
        }
        if (livesLinearLayout.getChildCount() == 0) {
            gameOver = true;
            if (score > highScore) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(HIGH_SCORE, score);
                editor.commit();
                highScore = score;
            }
            cancelAnimations();
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.game_over);
            builder.setMessage(resources.getString(R.string.score) + " " + score);
            builder.setPositiveButton(R.string.reset_game, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    displayScores();
                    dialogDisplayed = false;
                    resetGame();
                }
            });
            dialogDisplayed = true;
            builder.show();
        } else {
            removeOneLife();
            addNewSpot();
        }
    }

    public void increaseScore() {
        score += 10000 * level;
    }

    public void decreaseScore() {
        score -= 15 * level;
    }

    public void removeOneLife() {
        livesLinearLayout.removeViewAt(livesLinearLayout.getChildCount() - 1);
    }

    public void setSpotDiameter() {
        SPOT_DIAMETER = SPOT_DIAMETER * (int) getResources().getDisplayMetrics().density;
    }


}
