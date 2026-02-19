package it.unisa.trisense.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GyroMazeView extends SurfaceView implements Runnable, SurfaceHolder.Callback {

    private static final String TAG = "GyroMaze";

    private Thread gameThread;
    private boolean isPlaying;
    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private Random random;

    private boolean isGameStarted = false;
    private boolean isGameRunning = false;
    private boolean isPaused = false;
    private int score = 0;

    private float ballX, ballY;
    private float ballRadius = 30;
    private float ballSpeedX = 0, ballSpeedY = 0;
    private static final float SPEED_MULTIPLIER = 2.5f;
    private static final float DAMPING = 0.85f;

    private float sensorX = 0, sensorY = 0;

    private List<RectF> coins;
    private float coinRadius = 20;
    private static final int MAX_COINS = 5;
    private long lastCoinSpawnTime = 0;
    private static final long COIN_SPAWN_INTERVAL = 2000;

    private List<RectF> obstacles;
    private static final int MAX_OBSTACLES = 8;
    private long lastObstacleSpawnTime = 0;
    private static final long OBSTACLE_SPAWN_INTERVAL = 3000;
    private int difficultyLevel = 1;

    private int screenWidth;
    private int screenHeight;

    private long pauseStartTime = 0;
    private long totalPausedTime = 0;

    private OnGameEventListener gameEventListener;

    public interface OnGameEventListener {
        void onGameOver(int score);

        void onScoreUpdate(int score);
    }

    public void setOnGameEventListener(OnGameEventListener listener) {
        this.gameEventListener = listener;
    }

    public GyroMazeView(Context context) {
        super(context);
        init();
    }

    public GyroMazeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        paint = new Paint();
        paint.setAntiAlias(true);
        random = new Random();
        coins = new ArrayList<>();
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

    // Callback del SurfaceHolder
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Superficie creata. isPlaying=" + isPlaying);
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
        Log.d(TAG, "Superficie distrutta");
    }

    public void updateSensor(float x, float y) {
        // Accelerometro: x è positivo quando inclinato a destra, y positivo quando
        // inclinato in basso
        // Neghiamo x perché inclinando a destra la pallina deve muoversi a destra (il
        // sensore restituisce x negativo)
        this.sensorX = -x;
        this.sensorY = y;
    }

    private void update() {
        if (screenHeight == 0 || screenWidth == 0)
            return;
        if (!isGameStarted || !isGameRunning)
            return;
        if (isPaused)
            return;

        long now = System.currentTimeMillis();

        // Inizializza la pallina al centro
        if (ballX == 0 && ballY == 0) {
            ballX = screenWidth / 2f;
            ballY = screenHeight / 2f;
        }

        // Aggiorna la velocità della pallina in base al sensore
        ballSpeedX += sensorX * SPEED_MULTIPLIER;
        ballSpeedY += sensorY * SPEED_MULTIPLIER;

        // Applica lo smorzamento
        ballSpeedX *= DAMPING;
        ballSpeedY *= DAMPING;

        // Aggiorna la posizione della pallina
        ballX += ballSpeedX;
        ballY += ballSpeedY;

        // Limiti dello schermo
        if (ballX - ballRadius < 0) {
            ballX = ballRadius;
            ballSpeedX = 0;
        }
        if (ballX + ballRadius > screenWidth) {
            ballX = screenWidth - ballRadius;
            ballSpeedX = 0;
        }
        if (ballY - ballRadius < 0) {
            ballY = ballRadius;
            ballSpeedY = 0;
        }
        if (ballY + ballRadius > screenHeight) {
            ballY = screenHeight - ballRadius;
            ballSpeedY = 0;
        }

        // Aumento della difficoltà ogni 10 monete
        difficultyLevel = 1 + (score / 10);

        // Genera monete
        if (coins.size() < MAX_COINS && now - lastCoinSpawnTime > COIN_SPAWN_INTERVAL) {
            spawnCoin();
            lastCoinSpawnTime = now;
        }

        // Genera ostacoli
        long adjustedInterval = Math.max(1500, OBSTACLE_SPAWN_INTERVAL - (difficultyLevel - 1) * 300L);
        int maxObs = Math.min(MAX_OBSTACLES + difficultyLevel, 15);
        if (obstacles.size() < maxObs && now - lastObstacleSpawnTime > adjustedInterval) {
            spawnObstacle();
            lastObstacleSpawnTime = now;
        }

        // Controlla la raccolta delle monete
        Iterator<RectF> coinIter = coins.iterator();
        while (coinIter.hasNext()) {
            RectF coin = coinIter.next();
            float dx = ballX - coin.centerX();
            float dy = ballY - coin.centerY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < ballRadius + coinRadius) {
                coinIter.remove();
                score++;
                if (gameEventListener != null) {
                    gameEventListener.onScoreUpdate(score);
                }
            }
        }

        // Controlla la collisione con gli ostacoli
        for (RectF obstacle : obstacles) {
            // Trova il punto più vicino del rettangolo al centro della pallina
            float closestX = Math.max(obstacle.left, Math.min(ballX, obstacle.right));
            float closestY = Math.max(obstacle.top, Math.min(ballY, obstacle.bottom));
            float dx = ballX - closestX;
            float dy = ballY - closestY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist < ballRadius) {
                Log.d(TAG, "Fine partita! Collisione al punteggio: " + score);
                isGameRunning = false;
                if (gameEventListener != null) {
                    gameEventListener.onGameOver(score);
                }
                return;
            }
        }
    }

    private void spawnCoin() {
        float padding = coinRadius + 60;
        float cx = padding + random.nextFloat() * (screenWidth - 2 * padding);
        float cy = padding + random.nextFloat() * (screenHeight - 2 * padding);

        // Assicura che non si sovrapponga agli ostacoli
        RectF coinRect = new RectF(cx - coinRadius, cy - coinRadius, cx + coinRadius, cy + coinRadius);
        for (RectF obs : obstacles) {
            if (RectF.intersects(coinRect, obs)) {
                return; // Salta questa generazione; riproverà al ciclo successivo
            }
        }
        coins.add(coinRect);
    }

    private void spawnObstacle() {
        float padding = 80;
        int type = random.nextInt(3);
        float w, h;

        switch (type) {
            case 0: // Quadrato piccolo
                w = 60 + random.nextInt(40);
                h = w;
                break;
            case 1: // Barra orizzontale
                w = 120 + random.nextInt(100);
                h = 30 + random.nextInt(20);
                break;
            default: // Barra verticale
                w = 30 + random.nextInt(20);
                h = 120 + random.nextInt(100);
                break;
        }

        float ox = padding + random.nextFloat() * (screenWidth - 2 * padding - w);
        float oy = padding + random.nextFloat() * (screenHeight - 2 * padding - h);

        RectF obsRect = new RectF(ox, oy, ox + w, oy + h);

        // Assicura che non si sovrapponga all'area iniziale della pallina (centro dello
        // schermo)
        float cx = screenWidth / 2f;
        float cy = screenHeight / 2f;
        float safeZone = 150;
        if (Math.abs(obsRect.centerX() - cx) < safeZone && Math.abs(obsRect.centerY() - cy) < safeZone) {
            return; // Non generare al centro
        }

        // Assicura che non collida con la posizione corrente della pallina
        float closestX = Math.max(obsRect.left, Math.min(ballX, obsRect.right));
        float closestY = Math.max(obsRect.top, Math.min(ballY, obsRect.bottom));
        float dx = ballX - closestX;
        float dy = ballY - closestY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < ballRadius + 50) {
            return; // Troppo vicino alla pallina
        }

        obstacles.add(obsRect);
    }

    private void draw() {
        if (!surfaceHolder.getSurface().isValid())
            return;

        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null)
            return;

        // Sfondo - blu scuro
        canvas.drawColor(Color.rgb(15, 15, 50));

        // Disegna la griglia per effetto visivo
        paint.setColor(Color.argb(25, 0, 255, 255));
        paint.setStrokeWidth(1);
        for (int x = 0; x < screenWidth; x += 50) {
            canvas.drawLine(x, 0, x, screenHeight, paint);
        }
        for (int y = 0; y < screenHeight; y += 50) {
            canvas.drawLine(0, y, screenWidth, y, paint);
        }

        if (!isGameStarted) {
            // Schermata "Tocca per iniziare"
            paint.setTextSize(80);
            paint.setColor(Color.YELLOW);
            String text = "TOCCA PER INIZIARE";
            float textWidth = paint.measureText(text);
            canvas.drawText(text, (screenWidth - textWidth) / 2f, screenHeight / 2f, paint);

            paint.setTextSize(36);
            paint.setColor(Color.rgb(200, 200, 200));
            String sub = "Inclina il telefono per muovere la pallina!";
            float subWidth = paint.measureText(sub);
            canvas.drawText(sub, (screenWidth - subWidth) / 2f, screenHeight / 2f + 60, paint);
        } else if (isGameRunning || isPaused) {
            // Disegna gli ostacoli
            for (RectF obstacle : obstacles) {
                // Bagliore
                paint.setColor(Color.argb(40, 255, 50, 50));
                canvas.drawRoundRect(
                        new RectF(obstacle.left - 4, obstacle.top - 4, obstacle.right + 4, obstacle.bottom + 4),
                        6, 6, paint);
                // Corpo dell'ostacolo
                paint.setColor(Color.rgb(255, 50, 80));
                canvas.drawRoundRect(obstacle, 4, 4, paint);
            }

            // Disegna le monete
            for (RectF coin : coins) {
                // Bagliore
                paint.setColor(Color.argb(60, 255, 215, 0));
                canvas.drawCircle(coin.centerX(), coin.centerY(), coinRadius + 8, paint);
                // Corpo della moneta
                paint.setColor(Color.rgb(255, 215, 0));
                canvas.drawCircle(coin.centerX(), coin.centerY(), coinRadius, paint);
                // Riflesso interno
                paint.setColor(Color.argb(100, 255, 255, 200));
                canvas.drawCircle(coin.centerX() - 5, coin.centerY() - 5, coinRadius * 0.4f, paint);
            }

            // Disegna la pallina
            // Bagliore
            paint.setColor(Color.argb(50, 0, 255, 255));
            canvas.drawCircle(ballX, ballY, ballRadius + 12, paint);
            // Corpo della pallina
            paint.setColor(Color.CYAN);
            canvas.drawCircle(ballX, ballY, ballRadius, paint);
            // Riflesso interno
            paint.setColor(Color.argb(120, 255, 255, 255));
            canvas.drawCircle(ballX - ballRadius * 0.25f, ballY - ballRadius * 0.25f,
                    ballRadius * 0.35f, paint);

            // Punteggio
            paint.setColor(Color.WHITE);
            paint.setTextSize(50);
            canvas.drawText("Monete: " + score, 50, 80, paint);

            paint.setTextSize(36);
            paint.setColor(Color.rgb(180, 180, 180));
            canvas.drawText("Livello: " + difficultyLevel, 50, 130, paint);

            // Testo di pausa
            if (isPaused) {
                paint.setTextSize(80);
                paint.setColor(Color.YELLOW);
                String pauseText = "PAUSA";
                float tw = paint.measureText(pauseText);
                canvas.drawText(pauseText, (screenWidth - tw) / 2f, screenHeight / 2f, paint);
            }
        } else if (!isGameRunning && isGameStarted) {
            // Fine partita disegnata sul canvas (l'overlay gestirà la UI principale)
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
                isGameStarted = true;
                isGameRunning = true;
                ballX = screenWidth / 2f;
                ballY = screenHeight / 2f;
                ballSpeedX = 0;
                ballSpeedY = 0;
                lastCoinSpawnTime = System.currentTimeMillis();
                lastObstacleSpawnTime = System.currentTimeMillis();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    public void start() {
        Log.d(TAG, "Avvio chiamato. isPlaying=" + isPlaying);
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

    public void pause() {
        isPaused = true;
        pauseStartTime = System.currentTimeMillis();
    }

    public void resumeGame() {
        if (isPaused) {
            long pausedDuration = System.currentTimeMillis() - pauseStartTime;
            totalPausedTime += pausedDuration;
            // Aggiusta i timer di generazione per evitare una raffica di generazioni dopo
            // la ripresa
            lastCoinSpawnTime += pausedDuration;
            lastObstacleSpawnTime += pausedDuration;
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
        difficultyLevel = 1;
        coins.clear();
        obstacles.clear();
        isGameRunning = false;
        isGameStarted = false;
        isPaused = false;
        ballX = 0;
        ballY = 0;
        ballSpeedX = 0;
        ballSpeedY = 0;
        sensorX = 0;
        sensorY = 0;
        totalPausedTime = 0;

        if (gameEventListener != null) {
            gameEventListener.onScoreUpdate(0);
        }

        // Forza il ridisegno
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.rgb(15, 15, 50));
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
