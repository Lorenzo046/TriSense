package it.unisa.trisense.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;

public class FlashReflexView extends SurfaceView implements Runnable, SurfaceHolder.Callback {

    private static final String TAG = "FlashReflex";

    private Thread gameThread;
    private boolean isPlaying;
    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private Random random;

    // Game state
    private boolean isGameStarted = false;
    private boolean isGameRunning = false;
    private int score = 0;
    private int difficultyLevel = 1; // Increases every 10 taps

    // Circle properties
    private float circleX, circleY;
    private float circleRadius = 80;
    private int circleColor;
    private boolean circleVisible = false;
    private long circleSpawnTime;
    private long circleDisplayDuration; // ms - decreases as game progresses
    private boolean circleTapped = false; // Whether current circle was tapped

    // Timing
    private long pauseBetweenRounds = 600; // ms pause between circles
    private long lastCircleEndTime = 0;

    // Screen
    private int screenWidth;
    private int screenHeight;

    // Colors for circles
    private final int[] CIRCLE_COLORS = {
            Color.CYAN,
            Color.MAGENTA,
            Color.YELLOW,
            Color.GREEN,
            Color.rgb(255, 165, 0), // Orange
            Color.RED,
            Color.rgb(0, 255, 127), // Spring green
            Color.rgb(255, 105, 180) // Hot pink
    };

    // Feedback
    private String feedbackText = "";
    private long feedbackTime = 0;
    private int feedbackColor = Color.WHITE;

    // Listener
    private OnGameEventListener gameEventListener;

    public interface OnGameEventListener {
        void onGameOver(int score);

        void onScoreUpdate(int score);
    }

    public void setOnGameEventListener(OnGameEventListener listener) {
        this.gameEventListener = listener;
    }

    public FlashReflexView(Context context) {
        super(context);
        init();
    }

    public FlashReflexView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        paint = new Paint();
        paint.setAntiAlias(true);
        random = new Random();
        setFocusable(true);
        setZOrderMediaOverlay(true);
        getHolder().setFormat(android.graphics.PixelFormat.RGBA_8888);
    }

    @Override
    public void run() {
        while (isPlaying) {
            update();
            draw();
            control();
        }
    }

    // SurfaceHolder Callbacks
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Surface Created. isPlaying=" + isPlaying);
        if (isPlaying) {
            if (gameThread == null || !gameThread.isAlive()) {
                gameThread = new Thread(this);
                gameThread.start();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface Destroyed");
    }

    private void update() {
        if (screenHeight == 0 || screenWidth == 0)
            return;
        if (!isGameStarted || !isGameRunning)
            return;

        long now = System.currentTimeMillis();

        // Circle is visible - check if time expired
        if (circleVisible) {
            if (now - circleSpawnTime >= circleDisplayDuration) {
                // Circle expired without being tapped -> GAME OVER
                if (!circleTapped) {
                    feedbackText = "MANCATO!";
                    feedbackColor = Color.RED;
                    feedbackTime = now;
                    circleVisible = false;
                    isGameRunning = false;
                    if (gameEventListener != null) {
                        gameEventListener.onGameOver(score);
                    }
                    return;
                }
                circleVisible = false;
                lastCircleEndTime = now;
            }
        } else {
            // Circle not visible - wait for pause then spawn next
            if (now - lastCircleEndTime >= pauseBetweenRounds) {
                spawnCircle();
            }
        }
    }

    private void spawnCircle() {
        circleTapped = false;

        // Calculate difficulty level: increases every 10 successful taps
        difficultyLevel = 1 + (score / 10);

        // Calculate display duration based on difficulty
        // Level 1: 1500ms, decreases by 100ms per level, minimum 400ms
        circleDisplayDuration = Math.max(400, 1500 - ((difficultyLevel - 1) * 100L));

        // Random position (with padding so circle stays fully on screen)
        float padding = circleRadius + 40;
        float topPadding = padding + 150; // Extra top padding for score text
        circleX = padding + random.nextFloat() * (screenWidth - 2 * padding);
        circleY = topPadding + random.nextFloat() * (screenHeight - topPadding - padding);

        // Random color
        circleColor = CIRCLE_COLORS[random.nextInt(CIRCLE_COLORS.length)];

        circleVisible = true;
        circleSpawnTime = System.currentTimeMillis();

        Log.d(TAG, "Spawned circle at (" + circleX + "," + circleY
                + ") duration=" + circleDisplayDuration + "ms level=" + difficultyLevel);
    }

    private void draw() {
        if (!surfaceHolder.getSurface().isValid())
            return;

        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null)
            return;

        long now = System.currentTimeMillis();

        // Background - dark
        canvas.drawColor(Color.rgb(20, 20, 50));

        // Score and round info
        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        canvas.drawText("Score: " + score, 50, 80, paint);

        paint.setTextSize(36);
        paint.setColor(Color.rgb(180, 180, 180));
        canvas.drawText("Livello: " + difficultyLevel, 50, 130, paint);

        if (!isGameStarted) {
            // "Tap to Start" screen
            paint.setTextSize(80);
            paint.setColor(Color.YELLOW);
            String text = "TOCCA PER INIZIARE";
            float textWidth = paint.measureText(text);
            canvas.drawText(text, (screenWidth - textWidth) / 2f, screenHeight / 2f, paint);

            paint.setTextSize(36);
            paint.setColor(Color.rgb(200, 200, 200));
            String sub = "Tocca i cerchi prima che spariscano!";
            float subWidth = paint.measureText(sub);
            canvas.drawText(sub, (screenWidth - subWidth) / 2f, screenHeight / 2f + 60, paint);
        } else if (isGameRunning) {
            // Draw countdown bar for current circle
            if (circleVisible) {
                float elapsed = now - circleSpawnTime;
                float remaining = 1f - (elapsed / (float) circleDisplayDuration);
                remaining = Math.max(0, Math.min(1, remaining));

                // Timer bar at top
                paint.setColor(remaining > 0.3f ? Color.GREEN : Color.RED);
                canvas.drawRect(0, 0, screenWidth * remaining, 8, paint);

                // Draw the circle with glow effect
                // Outer glow
                paint.setColor(Color.argb(60, Color.red(circleColor),
                        Color.green(circleColor), Color.blue(circleColor)));
                canvas.drawCircle(circleX, circleY, circleRadius + 20, paint);

                // Main circle
                paint.setColor(circleColor);
                canvas.drawCircle(circleX, circleY, circleRadius, paint);

                // Inner highlight
                paint.setColor(Color.argb(80, 255, 255, 255));
                canvas.drawCircle(circleX - circleRadius * 0.25f,
                        circleY - circleRadius * 0.25f, circleRadius * 0.35f, paint);
            }

            // Draw feedback text
            if (now - feedbackTime < 500 && !feedbackText.isEmpty()) {
                paint.setTextSize(60);
                paint.setColor(feedbackColor);
                float tw = paint.measureText(feedbackText);
                canvas.drawText(feedbackText, (screenWidth - tw) / 2f, screenHeight / 2f + 200, paint);
            }
        } else if (!isGameRunning && isGameStarted) {
            // Game over state (drawn before overlay appears)
            paint.setTextSize(80);
            paint.setColor(Color.RED);
            String text = "GAME OVER";
            float tw = paint.measureText(text);
            canvas.drawText(text, (screenWidth - tw) / 2f, screenHeight / 2f, paint);
        }

        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    private void control() {
        try {
            Thread.sleep(17); // ~60 FPS
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isGameStarted) {
                // Start the game
                isGameStarted = true;
                isGameRunning = true;
                lastCircleEndTime = System.currentTimeMillis();
                return true;
            }

            if (isGameRunning && circleVisible && !circleTapped) {
                float touchX = event.getX();
                float touchY = event.getY();

                // Check if touch is inside the circle
                float dx = touchX - circleX;
                float dy = touchY - circleY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance <= circleRadius + 20) { // Small tolerance
                    // Hit!
                    circleTapped = true;
                    score++;
                    circleVisible = false;
                    lastCircleEndTime = System.currentTimeMillis();

                    feedbackText = "+1";
                    feedbackColor = Color.GREEN;
                    feedbackTime = System.currentTimeMillis();

                    if (gameEventListener != null) {
                        gameEventListener.onScoreUpdate(score);
                    }

                    Log.d(TAG, "Circle tapped! Score: " + score);
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    public void start() {
        Log.d(TAG, "Start called. isPlaying=" + isPlaying);
        isPlaying = true;

        if (gameThread != null && gameThread.isAlive()) {
            return;
        }

        if (surfaceHolder != null && surfaceHolder.getSurface().isValid()) {
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    public void stop() {
        isPlaying = false;
        try {
            if (gameThread != null) {
                gameThread.join();
                gameThread = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void reset() {
        score = 0;
        difficultyLevel = 1;
        circleVisible = false;
        circleTapped = false;
        isGameRunning = false;
        isGameStarted = false;
        feedbackText = "";
        lastCircleEndTime = 0;

        if (gameEventListener != null) {
            gameEventListener.onScoreUpdate(0);
        }

        // Force redraw
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.rgb(20, 20, 50));
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
    }
}
