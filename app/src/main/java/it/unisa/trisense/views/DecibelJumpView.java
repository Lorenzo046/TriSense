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

    private Rect player;
    private List<Rect> obstacles;
    private int playerY;
    private int playerX = 100;
    private int groundLevel;
    private int jumpHeight = 0;
    private boolean isJumping = false;

    private static final int GRAVITY = 2;
    private static final int JUMP_FORCE = 40;
    private static final int OBSTACLE_SPEED = 15;
    private static final int MIC_THRESHOLD = 5000;
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
        surfaceHolder.addCallback(this); // Registra callback
        paint = new Paint();
        obstacles = new ArrayList<>();
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
    }

    private long startTime;
    private boolean isGameStarted = false;
    private boolean isGameRunning = false;
    private boolean isPaused = false;
    private long pauseStartTime = 0;

    private void update() {
        if (screenHeight == 0 || screenWidth == 0)
            return;

        if (groundLevel == 0) {
            groundLevel = screenHeight - 200;
            playerY = groundLevel - 100;
            player = new Rect(playerX, playerY, playerX + 100, playerY + 100);
        }

        // Mostra "Tocca per iniziare"
        if (!isGameStarted) {
            return;
        }

        if (!isGameRunning)
            return;

        if (isPaused)
            return;

        // ... logica del gioco ...

        // Logica del salto (Audio)
        int amplitude = getAmplitude();
        if (amplitude > MIC_THRESHOLD && !isJumping) {
            isJumping = true;
            jumpHeight = JUMP_FORCE;
        }

        // Fisica del salto
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

        // Ostacoli (aspetta due secondi di attesa prima di spawnare un nuovo ostacolo)
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


            if (Rect.intersects(player, obstacle)) {
                Rect intersection = new Rect(player);
                if (intersection.intersect(obstacle)) {
                    // Controlla se l'intersezione è significativa è più di 20x20
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

    // ... (disegna e controlla i metodi se rimangono gli stessi)

    private void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas == null) {
                Log.e("DecibelJump", "Canvas is null despite valid surface!");
                return;
            }

            // Background
            canvas.drawColor(Color.rgb(30, 30, 80));

            if (groundLevel > 0) {
                // Terreno
                paint.setColor(Color.WHITE);
                canvas.drawRect(0, groundLevel, screenWidth, screenHeight, paint);

                // Player
                if (player != null) {
                    paint.setColor(Color.CYAN);
                    canvas.drawRect(player, paint);
                }

                // Ostacoli
                paint.setColor(Color.MAGENTA);
                for (Rect obstacle : obstacles) {
                    canvas.drawRect(obstacle, paint);
                }
            }

            // Punteggio
            paint.setColor(Color.WHITE);
            paint.setTextSize(60);
            canvas.drawText("Score: " + score, 50, 100, paint);

            // Pronti / Tocca per iniziare
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

    public void pause() {
        isPaused = true;
        pauseStartTime = System.currentTimeMillis();
    }

    public void resumeGame() {
        if (isPaused) {
            long pausedDuration = System.currentTimeMillis() - pauseStartTime;
            startTime += pausedDuration;
        }
        isPaused = false;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean isGameActive() {
        return isGameStarted && isGameRunning && !isPaused;
    }

    public void reset() {
        score = 0;
        obstacles.clear();
        isJumping = false;
        isGameRunning = false;
        isGameStarted = false; // Chiede di cliccare di nuovo
        isPaused = false;

        if (groundLevel > 0) {
            // ...
            playerY = groundLevel - 100;
            if (player != null) {
                player.top = playerY;
                player.bottom = playerY + 100;
            }
        }

        // isPlaying è gestito da start/stop
        if (gameEventListener != null) {
            gameEventListener.onScoreUpdate(0);
        }
        // Forza il redraw
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.rgb(30, 30, 80)); // Pulisce lo schermo con nuovi colori
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    // ... (startRecording, stopRecording, getAmplitude, onSizeChanged)

    private void startRecording() {
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            // Usa un file reale nelle directory delle cache
            String filePath = getContext().getExternalCacheDir().getAbsolutePath() + "/audiorecordtest.3gp";
            mediaRecorder.setOutputFile(filePath);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (IOException | IllegalStateException e) {
                e.printStackTrace();
                stopRecording();
            } catch (SecurityException e) {
                Log.e("DecibelJump", "Permission denied for audio recording", e);
                stopRecording();
            } catch (RuntimeException e) {
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
