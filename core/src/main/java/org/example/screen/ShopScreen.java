package org.example.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import org.example.Fonts;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import com.badlogic.gdx.audio.Sound;
import org.example.FishingVillageGame;
import org.example.fish.Rarity;
import org.example.game.CaughtFish;
import org.example.game.FishBag;
import org.example.gear.BaitType;
import org.example.gear.RodTier;

import java.util.List;
import java.util.Map;

public class ShopScreen extends ScreenAdapter {

    private enum Tab { SELL, GEAR }

    private static final float W  = 1280f;
    private static final float H  = 720f;
    private static final float PX = W / 2f - 360f;
    private static final float PY = H / 2f - 260f;
    private static final float PW = 720f;
    private static final float PH = 520f;
    private static final float CX = PX + 24f;

    private static final float SELL_ROW_Y0    = PY + PH - 110f;
    private static final float SELL_ROW_STEP  = -28f;
    private static final int   SELL_VISIBLE   = 10;

    private static final int GEAR_ITEM_COUNT = 6;

    private static final Color COIN_GOLD  = new Color(1.00f, 0.82f, 0.15f, 1f);
    private static final Color SEL_BG     = new Color(0.12f, 0.18f, 0.30f, 1f);
    private static final Color CANT_BUY   = new Color(0.35f, 0.35f, 0.38f, 1f);
    private static final Color OWNED      = new Color(0.30f, 0.75f, 0.30f, 1f);
    private static final Color ROW_WEIGHT = new Color(0.65f, 0.65f, 0.65f, 1f);
    private static final Color SEL_WEIGHT = new Color(0.90f, 0.90f, 0.90f, 1f);
    private static final Color COIN_DIM   = new Color(0.70f, 0.60f, 0.12f, 1f);
    private static final Color EMPTY_ROW  = new Color(0.20f, 0.20f, 0.24f, 1f);

    private static final Map<Rarity, Color> RARITY_COL = Map.of(
        Rarity.COMMON,    new Color(0.75f, 0.75f, 0.75f, 1f),
        Rarity.UNCOMMON,  new Color(0.30f, 0.85f, 0.30f, 1f),
        Rarity.RARE,      new Color(0.30f, 0.55f, 1.00f, 1f),
        Rarity.EPIC,      new Color(0.75f, 0.30f, 0.90f, 1f),
        Rarity.LEGENDARY, new Color(1.00f, 0.82f, 0.10f, 1f)
    );

    private final FishingVillageGame game;

    private OrthographicCamera camera;
    private ShapeRenderer      shapes;
    private SpriteBatch        batch;
    private BitmapFont         font;

    private Tab   currentTab       = Tab.SELL;
    private int   sellCursor       = 0;
    private int   sellScrollOffset = 0;
    private int   gearCursor       = 0;
    private Sound coinsSound;

    public ShopScreen(FishingVillageGame game) {
        this.game = game;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, W, H);
        shapes = new ShapeRenderer();
        batch  = new SpriteBatch();
        font   = Fonts.ui;
        coinsSound = Gdx.audio.newSound(Gdx.files.internal("coins.wav"));
    }

    @Override
    public void render(float delta) {
        handleInput();

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapes.setProjectionMatrix(camera.combined);

        drawPanel();
        drawContent();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            game.setScreen(game.fishingScreen);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            currentTab = (currentTab == Tab.SELL) ? Tab.GEAR : Tab.SELL;
            return;
        }
        if (currentTab == Tab.SELL) handleSellInput();
        else                        handleGearInput();
    }

    private void handleSellInput() {
        FishBag bag = game.gameState.getFishBag();
        if (!bag.isEmpty()) sellCursor = Math.max(0, Math.min(sellCursor, bag.size() - 1));

        if (!bag.isEmpty()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP))
                sellCursor = Math.max(0, sellCursor - 1);
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
                sellCursor = Math.min(bag.size() - 1, sellCursor + 1);
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                CaughtFish sold = bag.remove(sellCursor);
                game.gameState.addCoins(sold.value());
                sellCursor = Math.max(0, Math.min(sellCursor, bag.size() - 1));
                coinsSound.play(0.45f);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.A) && !bag.isEmpty()) {
            game.gameState.addCoins(bag.totalValue());
            bag.clear();
            sellCursor      = 0;
            sellScrollOffset = 0;
            coinsSound.play(0.45f);
        }

        // keep scroll window around the cursor
        if (sellCursor < sellScrollOffset) sellScrollOffset = sellCursor;
        if (sellCursor >= sellScrollOffset + SELL_VISIBLE) sellScrollOffset = sellCursor - SELL_VISIBLE + 1;
        int maxOffset = Math.max(0, bag.getMaxSlots() - SELL_VISIBLE);
        sellScrollOffset = Math.max(0, Math.min(sellScrollOffset, maxOffset));
    }

    private void handleGearInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP))
            gearCursor = Math.max(0, gearCursor - 1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
            gearCursor = Math.min(GEAR_ITEM_COUNT - 1, gearCursor + 1);

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            switch (gearCursor) {
                case 0 -> game.gameState.buyRod(RodTier.ADVANCED);
                case 1 -> game.gameState.buyRod(RodTier.MASTER);
                case 2 -> game.gameState.buyBait(BaitType.STANDARD);
                case 3 -> game.gameState.buyBait(BaitType.PREMIUM);
                case 4 -> game.gameState.buyBait(BaitType.EXOTIC);
                case 5 -> game.gameState.buyBagUpgrade();
            }
        }

        // E = equip highlighted bait
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            BaitType target = switch (gearCursor) {
                case 2 -> BaitType.STANDARD;
                case 3 -> BaitType.PREMIUM;
                case 4 -> BaitType.EXOTIC;
                default -> null;
            };
            if (target != null && game.gameState.getBaitCount(target) > 0) {
                game.gameState.setEquippedBait(target);
            }
        }
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    private void drawPanel() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.08f, 0.09f, 0.15f, 1f);
        shapes.rect(PX, PY, PW, PH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.45f, 0.40f, 0.20f, 1f);
        shapes.rect(PX, PY, PW, PH);
        shapes.rect(PX + 2f, PY + 2f, PW - 4f, PH - 4f);
        shapes.end();
    }

    private void drawContent() {
        // Selection highlight (before text)
        drawSelectionHighlight();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Title
        font.getData().setScale(2.4f);
        font.setColor(COIN_GOLD);
        font.draw(batch, "Village Shop", CX, PY + PH - 18f);
        font.getData().setScale(1f);

        // Coins
        font.setColor(0.72f, 0.72f, 0.72f, 1f);
        font.draw(batch, "Coins:", CX, PY + PH - 60f);
        font.getData().setScale(1.5f);
        font.setColor(COIN_GOLD);
        font.draw(batch, String.valueOf(game.gameState.getCoins()), CX + 64f, PY + PH - 57f);
        font.getData().setScale(1f);

        // Tab bar
        float tabY = PY + PH - 86f;
        font.getData().setScale(1.2f);
        font.setColor(currentTab == Tab.SELL ? COIN_GOLD : CANT_BUY);
        font.draw(batch, "[ SELL ]", CX, tabY);
        font.setColor(currentTab == Tab.GEAR ? COIN_GOLD : CANT_BUY);
        font.draw(batch, "[ GEAR ]", CX + 160f, tabY);
        font.getData().setScale(1f);
        font.setColor(0.28f, 0.25f, 0.10f, 1f);
        font.draw(batch, "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -", CX, PY + PH - 103f);

        if (currentTab == Tab.SELL) drawSellContent();
        else                        drawGearContent();

        font.setColor(Color.WHITE);
        batch.end();
    }

    private void drawSelectionHighlight() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(SEL_BG);
        if (currentTab == Tab.SELL && !game.gameState.getFishBag().isEmpty()) {
            float sy = SELL_ROW_Y0 + (sellCursor - sellScrollOffset) * SELL_ROW_STEP;
            shapes.rect(PX + 6f, sy - 12f, PW - 12f, 24f);
        } else if (currentTab == Tab.GEAR) {
            float gy = gearRowY(gearCursor);
            shapes.rect(PX + 6f, gy - 18f, PW - 12f, 36f);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ── Sell tab ──────────────────────────────────────────────────────────────

    private void drawSellContent() {
        FishBag bag = game.gameState.getFishBag();
        List<CaughtFish> slots = bag.getSlots();
        int maxSlots    = bag.getMaxSlots();
        int visibleEnd  = Math.min(sellScrollOffset + SELL_VISIBLE, maxSlots);

        // scroll-up indicator
        if (sellScrollOffset > 0) {
            font.setColor(0.55f, 0.55f, 0.60f, 1f);
            font.draw(batch, "^  more above", CX + 260f, SELL_ROW_Y0 + 16f);
        }

        for (int i = sellScrollOffset; i < visibleEnd; i++) {
            float ry  = SELL_ROW_Y0 + (i - sellScrollOffset) * SELL_ROW_STEP;
            boolean sel = !bag.isEmpty() && i == sellCursor;

            font.setColor(0.38f, 0.38f, 0.42f, 1f);
            font.draw(batch, (i + 1) + ".", CX, ry);

            if (i < slots.size()) {
                CaughtFish c = slots.get(i);
                if (c.isJunk()) {
                    font.setColor(sel ? new Color(0.88f, 0.80f, 0.62f, 1f) : new Color(0.52f, 0.46f, 0.36f, 1f));
                    font.draw(batch, c.displayName(), CX + 30f, ry);
                    font.setColor(sel ? COIN_GOLD : COIN_DIM);
                    font.draw(batch, c.value() + " coin", CX + 344f, ry);
                } else {
                    font.setColor(sel ? Color.WHITE : RARITY_COL.getOrDefault(c.fish().rarity(), Color.WHITE));
                    font.draw(batch, c.fish().name(), CX + 30f, ry);
                    font.setColor(sel ? SEL_WEIGHT : ROW_WEIGHT);
                    font.draw(batch, String.format("%.2f kg", c.weight()), CX + 234f, ry);
                    font.setColor(sel ? COIN_GOLD : COIN_DIM);
                    font.draw(batch, c.value() + " coins", CX + 344f, ry);
                    if (c.shiny()) {
                        font.setColor(1f, 0.88f, 0.10f, 1f);
                        font.draw(batch, "[S]", CX + 462f, ry);
                    }
                }
            } else {
                font.setColor(EMPTY_ROW);
                font.draw(batch, "---", CX + 30f, ry);
            }
        }

        // scroll-down indicator
        if (visibleEnd < maxSlots) {
            float indicatorY = SELL_ROW_Y0 + (visibleEnd - sellScrollOffset) * SELL_ROW_STEP - 4f;
            font.setColor(0.55f, 0.55f, 0.60f, 1f);
            font.draw(batch, "v  more below", CX + 260f, indicatorY);
        }

        font.setColor(0.28f, 0.25f, 0.10f, 1f);
        font.draw(batch, "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -", CX, PY + 100f);

        int total = bag.totalValue();
        font.setColor(0.60f, 0.60f, 0.65f, 1f);
        font.draw(batch, "Bag total:", CX, PY + 76f);
        font.getData().setScale(1.4f);
        font.setColor(total > 0 ? COIN_GOLD : EMPTY_ROW);
        font.draw(batch, total + " coins", CX + 96f, PY + 79f);
        font.getData().setScale(1f);

        font.getData().setScale(1.1f);
        font.setColor(0.50f, 0.50f, 0.55f, 1f);
        if (bag.isEmpty()) {
            font.draw(batch, "[LEFT/RIGHT] Switch Tab   [TAB] Back to Fishing", CX + 60f, PY + 44f);
        } else {
            font.draw(batch, "[UP/DOWN] Select  [SPACE] Sell  [A] Sell All  [TAB] Back", CX + 20f, PY + 44f);
        }
        font.getData().setScale(1f);
    }

    // ── Gear tab ──────────────────────────────────────────────────────────────

    private void drawGearContent() {
        RodTier rod = game.gameState.getRod();

        font.getData().setScale(0.85f);
        font.setColor(0.48f, 0.48f, 0.52f, 1f);
        font.draw(batch, "ROD UPGRADES", CX, PY + PH - 124f);
        font.getData().setScale(1f);

        drawGearRodRow(gearRowY(0), RodTier.ADVANCED, rod, gearCursor == 0);
        drawGearRodRow(gearRowY(1), RodTier.MASTER,   rod, gearCursor == 1);

        font.getData().setScale(0.85f);
        font.setColor(0.48f, 0.48f, 0.52f, 1f);
        font.draw(batch, "BAIT  (buy x5)", CX, PY + PH - 216f);
        font.getData().setScale(1f);

        drawGearBaitRow(gearRowY(2), BaitType.STANDARD, gearCursor == 2);
        drawGearBaitRow(gearRowY(3), BaitType.PREMIUM,  gearCursor == 3);
        drawGearBaitRow(gearRowY(4), BaitType.EXOTIC,   gearCursor == 4);

        font.getData().setScale(0.85f);
        font.setColor(0.48f, 0.48f, 0.52f, 1f);
        font.draw(batch, "BAG UPGRADES", CX, PY + PH - 344f);
        font.getData().setScale(1f);

        drawGearBagRow(gearRowY(5), gearCursor == 5);

        // Controls
        font.setColor(0.28f, 0.25f, 0.10f, 1f);
        font.draw(batch, "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -", CX, PY + 100f);
        font.setColor(0.50f, 0.50f, 0.55f, 1f);
        font.draw(batch, "[UP/DOWN] Select   [SPACE] Buy   [E] Equip Bait   [TAB] Back", CX + 10f, PY + 78f);
        font.draw(batch, "[LEFT/RIGHT] Switch to Sell tab", CX + 10f, PY + 55f);
    }

    private void drawGearRodRow(float ry, RodTier tier, RodTier owned, boolean sel) {
        boolean isOwned   = owned.ordinal() >= tier.ordinal();
        boolean canBuy    = !isOwned && owned.ordinal() == tier.ordinal() - 1;
        boolean canAfford = game.gameState.getCoins() >= tier.cost;

        font.setColor(sel ? Color.WHITE : (isOwned ? OWNED : (canBuy && canAfford ? ROW_WEIGHT : CANT_BUY)));
        font.draw(batch, tier.displayName, CX + 20f, ry);

        if (isOwned) {
            font.setColor(OWNED);
            font.draw(batch, "OWNED", CX + 400f, ry);
        } else if (!canBuy) {
            font.setColor(CANT_BUY);
            font.draw(batch, "req. prev. tier", CX + 340f, ry);
        } else {
            font.setColor(canAfford ? (sel ? COIN_GOLD : COIN_DIM) : CANT_BUY);
            font.draw(batch, tier.cost + " coins", CX + 400f, ry);
            if (!canAfford) {
                font.setColor(CANT_BUY);
                font.draw(batch, "can't afford", CX + 520f, ry);
            }
        }
    }

    private void drawGearBaitRow(float ry, BaitType bait, boolean sel) {
        boolean canAfford  = game.gameState.getCoins() >= bait.totalCost();
        int     owned      = game.gameState.getBaitCount(bait);
        boolean isEquipped = game.gameState.getEquippedBait() == bait;

        font.setColor(sel ? Color.WHITE : ROW_WEIGHT);
        font.draw(batch, bait.displayName, CX + 20f, ry);

        font.setColor(canAfford ? (sel ? COIN_GOLD : COIN_DIM) : CANT_BUY);
        font.draw(batch, "x5  " + bait.totalCost() + "c", CX + 230f, ry);

        font.setColor(owned > 0 ? (sel ? Color.WHITE : ROW_WEIGHT) : CANT_BUY);
        font.draw(batch, "owned: " + owned, CX + 370f, ry);

        if (isEquipped) {
            font.setColor(COIN_GOLD);
            font.draw(batch, "[EQ]", CX + 510f, ry);
        }
    }

    private float gearRowY(int index) {
        return switch (index) {
            case 0 -> PY + PH - 148f;  // 472 - Advanced Rod
            case 1 -> PY + PH - 180f;  // 440 - Master Rod
            case 2 -> PY + PH - 240f;  // 380 - Std Bait
            case 3 -> PY + PH - 274f;  // 346 - Prem Bait
            case 4 -> PY + PH - 308f;  // 312 - Exotic Bait
            case 5 -> PY + PH - 368f;  // 252 - Bag Upgrade
            default -> PY + PH - 148f;
        };
    }

    private void drawGearBagRow(float ry, boolean sel) {
        int   level  = game.gameState.getBagUpgradeLevel();
        int[] costs  = {500, 1000, 2000, 4000};
        boolean maxed = level >= costs.length;

        if (maxed) {
            font.setColor(OWNED);
            font.draw(batch, "Bag  (30 slots — fully upgraded)", CX + 20f, ry);
            font.setColor(OWNED);
            font.draw(batch, "MAX", CX + 560f, ry);
        } else {
            int current   = game.gameState.getFishBag().getMaxSlots();
            int cost      = costs[level];
            boolean canAfford = game.gameState.getCoins() >= cost;
            font.setColor(sel ? Color.WHITE : ROW_WEIGHT);
            font.draw(batch, "Bag upgrade  (" + current + " → " + (current + 5) + " slots)", CX + 20f, ry);
            font.setColor(canAfford ? (sel ? COIN_GOLD : COIN_DIM) : CANT_BUY);
            font.draw(batch, cost + " coins", CX + 400f, ry);
            if (!canAfford) {
                font.setColor(CANT_BUY);
                font.draw(batch, "can't afford", CX + 510f, ry);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, W, H);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        coinsSound.dispose();
    }
}
