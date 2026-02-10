package it.unisa.trisense.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class DecibelJumpView extends SurfaceView implements Runnable, SurfaceHolder.Callback {

    private Thread gameThread;
    private boolean isPlaying;
    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private MediaRecorder mediaRecorder;

    // Game Objects
    private Rect player;
    private List<Rect> obstacles;
    private int playerY;
    private int playerX = 100;
    private int groundLevel;
    private int jumpHeight = 0;
    private boolean isJumping = false;

    // Game Config
    private static final int GRAVITY = 2; // Reduced gravity
    private static final int JUMP_FORCE = 40; // Increased jump initial force
    private static final int OBSTACLE_SPEED = 15;
    private static final int MIC_THRESHOLD = 5000; // Adjust sensitivity
    private int screenWidth;
    private int screenHeight;
    private int score = 0;

    private OnGameEventListener gameEventListener;

    public interface OnGameEventListener {
        void onGameOver(int score);

        void onScoreUpdate(int score);
    }

    public void setOnGameEventListener(OnGameEventListener listener) {
        this.gameEventListener = listener;
    }

    public DecibelJumpView(Context context) {
        super(context);
        init();
    }

    public DecibelJumpView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this); // Register callback
        paint = new Paint();
        obstacles = new ArrayList<>();
        setFocusable(true); // Ensure view can take focus
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
        Log.d("DecibelJump", "Surface Created. isPlaying=" + isPlaying);
        if (isPlaying) {
            if (gameThread == null || !gameThread.isAlive()) {
                Log.d("DecibelJump", "Starting thread from surfaceCreated.");
                gameThread = new Thread(this);
                gameThread.start();
                startRecording();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("DecibelJump", "Surface Changed: " + width + "x" + height);
        screenWidth = width;
        screenHeight = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("DecibelJump", "Surface Destroyed");
        boolean retry = true;
        // isPlaying can remain true if we want to resume on create,
        // but typically valid surface is needed for thread.
        // We handle thread stopping in stop() method usually.
    }

    private long startTime;
    private boolean isGameStarted = false; // Waiting for user to tap start
    private boolean isGameRunning = false; // Game logic active

    private void update() {
        if (screenHeight == 0 || screenWidth == 0)
            return;

        // Ground Level Init
        if (groundLevel == 0) {
            groundLevel = screenHeight - 200;
            playerY = groundLevel - 100;
            player = new Rect(playerX, playerY, playerX + 100, playerY + 100);
            // Don't start time here anymore
        }

        // Show "Tap to Start" state
        if (!isGameStarted) {
            return;
        }

        if (!isGameRunning)
            return;

        // ... game logic ...

        // Jump Logic (Audio)
        int amplitude = getAmplitude();
        if (amplitude > MIC_THRESHOLD && !isJumping) {
            isJumping = true;
            jumpHeight = JUMP_FORCE;
        }

        // Physics
        if (isJumping) {
            playerY -= jumpHeight;
            jumpHeight -= GRAVITY;
            if (playerY >= groundLevel - 100) {
                playerY = groundLevel - 100;
                isJumping = false;
            }
        } else {
            if (playerY < groundLevel - 100) {
                playerY += GRAVITY * 2; // Fall faster
            }
        }
        player.top = playerY;
        player.bottom = playerY + 100;

        // Obstacles (Wait 2 seconds before first spawn)
        if (System.currentTimeMillis() - startTime > 2000) {
            if (obstacles.isEmpty() || obstacles.get(obstacles.size() - 1).left < screenWidth - 600) {
                spawnObstacle();
            }
        }

        Iterator<Rect> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Rect obstacle = iterator.next();
            obstacle.left -= OBSTACLE_SPEED;
            obstacle.right -= OBSTACLE_SPEED;

            // Debug Collision
            // Log.d("Game", "Player: " + player + " Obstacle: " + obstacle);

            if (Rect.intersects(player, obstacle)) {
                // Refine collision box - allow small overlap
                Rect intersection = new Rect(player);
                if (intersection.intersect(obstacle)) {
                    // Check if intersection is significant (e.g., more than 10 pixels)
                    if (intersection.width() > 20 && intersection.height() > 20) {
                        Log.d("DecibelJump", "Game Over! Collision at Score: " + score + " PlayerY: " + playerY);
                        isPlaying = false;
                        isGameRunning = false;
                        if (gameEventListener != null) {
                            gameEventListener.onGameOver(score);
                        }
                    }
                }
            }

            if (obstacle.right < 0) {
                iterator.remove();
                score++;
                if (gameEventListener != null) {
                    gameEventListener.onScoreUpdate(score);
                }
            }
        }
    }

    // ... (draw and control methods remain same)

    private void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            // Log.d("DecibelJump", "Drawing frame..."); // Uncomment if needed, spammy
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas == null) {
                Log.e("DecibelJump", "Canvas is null despite valid surface!");
                return;
            }

            // Background - Bright Blue for debug
            canvas.drawColor(Color.BLUE);

            // Log heartbeat every 60 frames (~1 sec)
            if (System.currentTimeMillis() % 1000 < 20) {
                Log.d("DecibelJump", "Drawing frame... isGameStarted=" + isGameStarted);
            }

            if (groundLevel > 0) {
                // Ground
                paint.setColor(Color.WHITE);
                canvas.drawRect(0, groundLevel, screenWidth, screenHeight, paint);

                // Player
                if (player != null) {
                    paint.setColor(Color.CYAN);
                    canvas.drawRect(player, paint);
                }

                // Obstacles
                paint.setColor(Color.MAGENTA);
                for (Rect obstacle : obstacles) {
                    canvas.drawRect(obstacle, paint);
                }
            }

            // Score
            paint.setColor(Color.WHITE);
            paint.setTextSize(60);
            canvas.drawText("Score: " + score, 50, 100, paint);

            // Amplitude Debug/Visualization
            int amp = getAmplitude();
            paint.setTextSize(40);
            canvas.drawText("Mic: " + amp + " / " + MIC_THRESHOLD, 50, 200, paint);

            // Visual Bar for Mic
            float barHeight = (float) amp / 32767 * 500; // Max amplitude is 32767
            paint.setColor(Color.GREEN);
            if (amp > MIC_THRESHOLD)
                paint.setColor(Color.RED); // Threshold hit
            canvas.drawRect(50, 250, 100, 250 + barHeight, paint);

            // Ready / Tap to Start Message
            if (!isGameStarted) {
                paint.setTextSize(80);
                paint.setColor(Color.YELLOW);
                String text = "TOCCA PER INIZIARE";
                float textWidth = paint.measureText(text);
                canvas.drawText(text, (screenWidth - textWidth) / 2, screenHeight / 2, paint);
            } else if (isGameRunning && System.currentTimeMillis() - startTime < 2000) {
                paint.setTextSize(100);
                paint.setColor(Color.YELLOW);
                String text = "PRONTI...";
                float textWidth = paint.measureText(text);
                canvas.drawText(text, (screenWidth - textWidth) / 2, screenHeight / 2, paint);
            }

            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void control() {
        try {
            Thread.sleep(17); // ~60 FPS
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void spawnObstacle() {
        if (screenWidth == 0)
            return;
        Random random = new Random();
        int height = 100 + random.nextInt(100);
        int top = groundLevel - height;

        // Spawn slightly off-screen to avoid pop-in
        int spawnX = screenWidth + 100;

        Rect obstacle = new Rect(spawnX, top, spawnX + 100, groundLevel);
        obstacles.add(obstacle);
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            if (!isGameStarted) {
                isGameStarted = true;
                isGameRunning = true;
                startTime = System.currentTimeMillis();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    public void start() {
        Log.d("DecibelJump", "Start called. isPlaying=" + isPlaying + " SurfaceValid="
                + (surfaceHolder != null && surfaceHolder.getSurface().isValid()));
        isPlaying = true;

        if (gameThread != null && gameThread.isAlive()) {
            Log.d("DecibelJump", "Thread already running.");
            return;
        }

        if (surfaceHolder != null && surfaceHolder.getSurface().isValid()) {
            Log.d("DecibelJump", "Surface valid, starting thread immediately.");
            gameThread = new Thread(this);
            gameThread.start();
            startRecording();
        } else {
            Log.d("DecibelJump", "Surface not valid yet, waiting for surfaceCreated.");
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
        stopRecording();
    }

    public void reset() {
        score = 0;
        obstacles.clear();
        isJumping = false;
        isGameRunning = false;
        isGameStarted = false; // Require tap again

        if (groundLevel > 0) {
            // ...
            playerY = groundLevel - 100;
            if (player != null) {
                player.top = playerY;
                player.bottom = playerY + 100;
            }
        }

        // isPlaying is managed by start/stop
        // When reset is called, we usually want to be ready to play
        if (gameEventListener != null) {
            gameEventListener.onScoreUpdate(0);
        }
        // Force redraw
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.rgb(30, 30, 80)); // Clear screen with new color
                // You might want to draw the initial state here
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    // ... (startRecording, stopRecording, getAmplitude, onSizeChanged remain same)

    private void startRecording() {
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            // Use a real file in cache directory
            String filePath = getContext().getExternalCacheDir().getAbsolutePath() + "/audiorecordtest.3gp";
            mediaRecorder.setOutputFile(filePath);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (IOException | IllegalStateException e) {
                e.printStackTrace();
                // Release if start fails
                stopRecording();
            } catch (RuntimeException e) {
                // Should not happen if permission is granted, but safety net
                e.printStackTrace();
                stopRecording();
            }
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                // Ignore if called immediately after start or if start failed
            }
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private int getAmplitude() {
        if (mediaRecorder != null) {
            return mediaRecorder.getMaxAmplitude();
        }
        return 0;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
    }
}
