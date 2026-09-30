package raffle.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Transparent overlay that shoots confetti from the two lower corners and rains some from the top.
 * It never takes mouse input, so it can sit on top of the draw screen.
 */
public final class ConfettiCanvas extends Canvas {

   private static final Color[] COLORS = {Color.web("#ffd700"), Color.web("#00ffea"), Color.web("#ffffff"), Color.web("#3498db"),
                                          Color.web("#ff6b6b"), Color.web("#7CFC00"), Color.web("#9c7945")};
   private static final double GRAVITY = 900;
   private static final double MAX_SECONDS = 7;

   private static final class Piece {
      double x, y, vx, vy, angle, spin, flip, flipSpeed, size;
      Color color;
      boolean round;
   }

   private final List<Piece> pieces = new ArrayList<>();
   private final Random random = new Random();
   private final AnimationTimer timer;
   private long lastNanos;
   private double elapsed;

   public ConfettiCanvas() {
      setMouseTransparent(true);
      setManaged(false);// sized by whoever adds it, never by the layout
      timer = new AnimationTimer() {
         @Override
         public void handle(long now) {
            double dt = Math.min(0.05, (now - lastNanos) / 1_000_000_000.0);
            lastNanos = now;
            step(dt);
         }
      };
   }

   /** Fires a new burst; a burst that is still running is replaced. */
   public void burst() {
      stop();
      double w = getWidth();
      double h = getHeight();
      if (w <= 0 || h <= 0) {
         return;
      }// end of if block

      double scale = Math.max(0.6, Math.min(w / 1000.0, h / 800.0));
      for (int i = 0; i < 110; i++) {// cannons in the lower corners, aimed at the middle
         boolean left = i % 2 == 0;
         Piece piece = newPiece(scale);
         piece.x = left ? w * 0.04 : w * 0.96;
         piece.y = h * 0.95;
         piece.vx = (left ? 1 : -1) * (250 + random.nextDouble() * 650) * scale;
         piece.vy = -(850 + random.nextDouble() * 650) * scale;
         pieces.add(piece);
      }// end of for loop
      for (int i = 0; i < 70; i++) {// rain from above
         Piece piece = newPiece(scale);
         piece.x = random.nextDouble() * w;
         piece.y = -random.nextDouble() * h * 0.5;
         piece.vx = (random.nextDouble() - 0.5) * 120 * scale;
         piece.vy = (80 + random.nextDouble() * 200) * scale;
         pieces.add(piece);
      }// end of for loop

      elapsed = 0;
      lastNanos = System.nanoTime();
      timer.start();
   }// end of burst method

   public void stop() {
      timer.stop();
      pieces.clear();
      GraphicsContext gc = getGraphicsContext2D();
      gc.clearRect(0, 0, getWidth(), getHeight());
   }// end of stop method

   private Piece newPiece(double scale) {
      Piece piece = new Piece();
      piece.size = (9 + random.nextDouble() * 9) * scale;
      piece.color = COLORS[random.nextInt(COLORS.length)];
      piece.angle = random.nextDouble() * 360;
      piece.spin = (random.nextDouble() - 0.5) * 720;
      piece.flip = random.nextDouble() * Math.PI * 2;
      piece.flipSpeed = 4 + random.nextDouble() * 8;
      piece.round = random.nextInt(5) == 0;
      return piece;
   }// end of newPiece method

   private void step(double dt) {
      elapsed += dt;
      double h = getHeight();
      GraphicsContext gc = getGraphicsContext2D();
      gc.clearRect(0, 0, getWidth(), h);

      pieces.removeIf(piece -> piece.y > h + 40);
      for (Piece piece : pieces) {
         piece.vy += GRAVITY * dt;
         piece.vx *= Math.pow(0.55, dt);// air drag: the burst spreads out, then drifts
         piece.vy = Math.min(piece.vy, 420);// terminal speed: pieces flutter down instead of dropping
         piece.x += piece.vx * dt;
         piece.y += piece.vy * dt;
         piece.angle += piece.spin * dt;
         piece.flip += piece.flipSpeed * dt;

         gc.save();
         gc.translate(piece.x, piece.y);
         gc.rotate(piece.angle);
         gc.scale(1, Math.cos(piece.flip));// paper turning over
         gc.setFill(piece.color);
         if (piece.round) {
            gc.fillOval(-piece.size / 2, -piece.size / 2, piece.size, piece.size);
         } else {
            gc.fillRect(-piece.size / 2, -piece.size * 0.3, piece.size, piece.size * 0.6);
         }// end of if-else block
         gc.restore();
      }// end of for loop

      if (pieces.isEmpty() || elapsed > MAX_SECONDS) {
         stop();
      }// end of if block
   }// end of step method

}// end of ConfettiCanvas class
