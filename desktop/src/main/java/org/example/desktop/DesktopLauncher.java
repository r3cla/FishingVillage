package org.example.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.example.FishingVillageGame;

public class DesktopLauncher {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Fishing Village");
        config.setWindowedMode(1280, 720);
        config.setMaximized(true);
        config.setForegroundFPS(60);
        config.useVsync(true);
        config.setWindowIcon("sprites/Fish/debugFesh.png");
        new Lwjgl3Application(new FishingVillageGame(), config);
    }
}
