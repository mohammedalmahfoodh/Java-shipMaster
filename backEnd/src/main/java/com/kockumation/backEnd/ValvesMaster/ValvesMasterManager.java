package com.kockumation.backEnd.ValvesMaster;

import com.kockumation.backEnd.global.GlobalVariableSingleton;
import com.kockumation.backEnd.levelMaster.LiveDataWebsocketClient;
import com.kockumation.backEnd.ValvesMaster.model.ValveDataForMap;
import com.kockumation.backEnd.utilities.GetValvesNames;
import org.json.simple.JSONObject;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class ValvesMasterManager extends Thread {

    private final String uri = "ws://192.168.190.232:8089";
   //  private final String uri = "ws://127.0.0.1:8089";
    GetValvesNames getValvesNames;
    AllValvesSetupData allValvesSetup;


    public static Map<Integer, ValveDataForMap> valveMapData = new HashMap<>();
    public static boolean ifAllValvesSetupDataInserted = false;
    public static boolean ifValveLiveDataSubscription = false;
    public static boolean checkIfDataExistsInValvesTable = false;
    LiveDataWebsocketClient liveDataWebsocketClient;

    @Override
    public void run() {
        valvesMasterEngine();
    }

    public ValvesMasterManager() {
        allValvesSetup = new AllValvesSetupData();
        getValvesNames = new GetValvesNames();

        liveDataWebsocketClient = new LiveDataWebsocketClient();
    }

    /// Valves Master Engine ********************************************************
    public void valvesMasterEngine() {

        //  Get All Valves Setup Data
        while (!ifAllValvesSetupDataInserted) {

            // System.out.println("Websocket not connected");
            JSONObject getKslTankData = new JSONObject();
            JSONObject vessel2 = new JSONObject();
            vessel2.put("vessel", 1);
            getKslTankData.put("getSmAllValvesSetupData", vessel2);
            String getAllValvesSetupDataString = getKslTankData.toString();
            try {
                allValvesSetup.createMapValvesWithNames();
                GlobalVariableSingleton.getInstance().getClient().connectToServer(allValvesSetup, new URI(uri));
                allValvesSetup.sendMessage(getAllValvesSetupDataString);

                try {
                    checkIfDataExistsInValvesTable = allValvesSetup.checkIfDataExists().get();
                       if (checkIfDataExistsInValvesTable){
                           allValvesSetup.updateValveSetup();
                           System.out.println("Valves setup updated now .");
                       }else {
                         //  System.out.println("Data existence is " + checkIfDataExistsInValvesTable);
                           allValvesSetup.insertValvesSettings();
                           System.out.println("Valves setup inserted into valve table.");
                       }


                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

            } catch (DeploymentException e) {
                // e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

        }

        // Subscribe to Valves live data   ************** Subscribe to Valves live data **********************
        while (true) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Not yet subscribed but valves setup is already inserted or updated
            if (!ifValveLiveDataSubscription && ifAllValvesSetupDataInserted) {
                 // iterateValvesMap();
                JSONObject valveSubscription = new JSONObject();
                JSONObject valveIdObject = new JSONObject();
                valveIdObject.put("tankId", 0);
                valveSubscription.put("setSmValveSubscriptionOn", valveIdObject);
                String valveSubscriptionStr = valveSubscription.toString();
                try {

                    GlobalVariableSingleton.getInstance().getClient().connectToServer(liveDataWebsocketClient, new URI(uri));
                    liveDataWebsocketClient.sendMessage(valveSubscriptionStr);


                } catch (DeploymentException e) {
                    DetectAndSaveValvesAlarms.timer.cancel();
                    DetectAndSaveValvesAlarms.timer.purge();
                    System.out.println("Live Data Websocket not ready start websocket server.");
                    ifValveLiveDataSubscription = false;
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }

            }

        }


    } // Subscribe to Valves live data   ************** Subscribe to Valves live data **********************

    public void iterateValvesMap() {
        valveMapData.entrySet().stream().forEach(e -> System.out.println(e.getKey() + ":" + e.getValue()));

    }


}
