/*
 * apc40-mode-win-linux
 *
 * Copyright (c) 2014 Semyon Proshev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import com.sun.media.sound.MidiOutDeviceProvider;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.sound.midi.*;

public class Main extends Application {
    private static final String APC40_NAME = "APC40";
    private static final String APC20_NAME = "APC20";

    private final Text myStatusText = new Text("Select mode");

    public static void main(String[] args)
            throws MidiUnavailableException, InvalidMidiDataException {
        launch(args);
    }

    @Override
    public void start(Stage stage)
            throws Exception {
        try {
            String osName = System.getProperty("os.name");
            if (!osName.startsWith("Windows") && !osName.startsWith("Linux") && !osName.startsWith("LINUX")) {
                showMessage(osName + " is not supported");
                return;
            }
        } catch (SecurityException e) {
            showMessage("Couldn't get information about operation system name");
            return;
        }
        HBox modesBox = new HBox();
        modesBox.setPadding(new Insets(12));
        modesBox.setSpacing(10);
        for (APCMode mode : APCMode.values()) {
            Button button = new Button(mode.toString());
            button.setOnAction(new ModeButtonClickHandler(mode));
            modesBox.getChildren().add(button);
        }
        VBox statusBox = new VBox();
        statusBox.setPadding(new Insets(5, 12, 5, 12));
        statusBox.setStyle("-fx-background-color: gainsboro");
        statusBox.getChildren().add(myStatusText);

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(modesBox);
        borderPane.setBottom(statusBox);

        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(new CloseRequestHandler());
    }

    private void showMessage(String message) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setScene(new Scene(VBoxBuilder.create().
                children(new Text(message)).
                alignment(Pos.CENTER).padding(new Insets(5)).build()));
        dialogStage.show();
    }

    private synchronized void applyMode(APCMode mode)
            throws MidiUnavailableException, InvalidMidiDataException {
        MidiDevice midiOutputDevice = getMidiOutputDevice();

        if (midiOutputDevice != null) {
            midiOutputDevice.open();
            Receiver midiOutputReceiver = midiOutputDevice.getReceiver();
            byte[] message = {(byte) 0xf0, 0x47, 0x00, 0x73, 0x60, 0x00, 0x04, mode.getByte(), 0x08, 0x04, 0x01, (byte) 0xf7};

            SysexMessage sysexMessage = new SysexMessage(message, message.length);
            midiOutputReceiver.send(sysexMessage, -1);
            midiOutputReceiver.close();
            midiOutputDevice.close();
            printMessage("\"" + mode.toString() + "\" is turned on");
        } else {
            printMessage("No supported device found");
        }
    }

    private MidiDevice getMidiOutputDevice()
            throws MidiUnavailableException {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        MidiOutDeviceProvider provider = new MidiOutDeviceProvider();

        System.out.println("Searching Mid-devices...");
        for (MidiDevice.Info info : infos) {
            System.out.println("MIDI Device Info => " + info.getName());
            String deviceName = info.getName();

            if ((deviceName.contains(APC40_NAME) || deviceName.contains(APC20_NAME)) && provider.isDeviceSupported(info)) {
                return MidiSystem.getMidiDevice(info);
            }
        }
        return null;
    }

    private void printMessage(String message) {
        System.out.println(message);
        myStatusText.setText(message);
    }

    private enum APCMode {
        Generic, AbletonLive, AlternateAbletonLive;

        public byte getByte() {
            switch (this) {
                case Generic:
                    return (byte) 0x40;
                case AbletonLive:
                    return (byte) 0x41;
                case AlternateAbletonLive:
                    return (byte) 0x42;
            }
            throw new RuntimeException("Unsupported enum: " + this);
        }

        @Override
        public String toString() {
            switch (this) {
                case Generic:
                    return "Generic Mode";
                case AbletonLive:
                    return "Ableton Live Mode";
                case AlternateAbletonLive:
                    return "Alternate Ableton Live Mode";
            }
            throw new RuntimeException("Unsupported enum: " + this);
        }
    }

    private class ModeButtonClickHandler implements EventHandler<ActionEvent> {
        private final APCMode mode;

        public ModeButtonClickHandler(APCMode mode) {
            this.mode = mode;
        }

        @Override
        public void handle(ActionEvent actionEvent) {
            try {
                applyMode(mode);
            } catch (MidiUnavailableException | InvalidMidiDataException e) {
                printMessage(e.getMessage());
            }
        }
    }

    private class CloseRequestHandler implements EventHandler<WindowEvent> {
        @Override
        public void handle(WindowEvent windowEvent) {
            Platform.exit();
            System.exit(0);
        }
    }
}