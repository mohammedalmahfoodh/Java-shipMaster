package com.kockumation.backEnd.services;

import com.kockumation.backEnd.global.Db;
import com.kockumation.backEnd.levelMaster.DetectAndSaveAlarms;
import com.kockumation.backEnd.levelMaster.model.TankDataForMap;
import com.kockumation.backEnd.model.Alarm;
import com.kockumation.backEnd.model.LevelPostObject;
import com.kockumation.backEnd.model.Tank;
import com.kockumation.backEnd.utilities.MySQLJDBCUtil;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class LevelMasterService {
    DetectAndSaveAlarms detectAndSaveAlarms = new DetectAndSaveAlarms();
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    //Make temp alarm Acknowledged ******************** Make temp alarm Acknowledged ****************************************************
    public boolean makeTempAlarmAcknowledged(int tank_id) {
        if (Db.tankMapData.containsKey(tank_id)) {
            TankDataForMap tankDataForMap = Db.tankMapData.get(tank_id);
            if ((tankDataForMap.isTemp_alarm_active() == true || tankDataForMap.isTemp_blue_alarm() == true) && tankDataForMap.isTemp_acknowledged() == false) {

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String temperatureAlarmDate = LocalDateTime.now(Clock.systemUTC()).format(formatter);
                tankDataForMap.setTemp_time_accepted(temperatureAlarmDate);
                tankDataForMap.setTemp_alarm_description("Temp Alarm Active accepted");
                tankDataForMap.setTemp_acknowledged(true);
                if (tankDataForMap.isTemp_inserted() == true) {

                    boolean insertedOrNot = false;
                    try {
                        insertedOrNot = detectAndSaveAlarms.updateTempAlarm(tankDataForMap).get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    if (insertedOrNot) {

                        System.out.println("Temp Alarm for tank " + tankDataForMap.getCode_name() + " became Accepted");
                        return true;
                    } else {
                        System.out.println("Error connecting database connect to db to update alarms");
                        return false;
                    }
                }
            } else {
                return false;
            }

        } else { // No such temp with this id
            return false;
        }

        return true;
    }//Make temp alarm Acknowledged ******************** Make temp alarm Acknowledged ****************************************************


    //Get  a list of latest 100 alarms from MySql. ******************** Get  a list of latest 100 alarms from MySql. ****************************************************
    public Future<List<Alarm>> getHundredAlarms() {
        List<Alarm> alarmList = new ArrayList<>();
        try (Connection conn = MySQLJDBCUtil.getConnection()) {
            String query = "SELECT al.alarm_id,temp_alarm,al.alarm_name,al.alarm_active,al.acknowledged,al.alarm_date,al.alarm_description,al.archive,al.blue_alarm,al.pump_id,al.tank_id,al.time_accepted,al.time_retrieved,al.valve_id FROM alarms al order by  alarm_active desc ,acknowledged asc ,archive asc ,  alarm_date desc LIMIT 100";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                Alarm alarm = new Alarm();
                alarm.setAlarm_name(rs.getString("alarm_name"));
                alarm.setAlarm_id(rs.getInt("alarm_id"));
                alarm.setAlarm_id(rs.getInt("temp_alarm"));
                alarm.setAlarm_active(rs.getInt("alarm_active"));
                alarm.setAcknowledged(rs.getInt("acknowledged"));
                alarm.setAlarm_date(rs.getString("alarm_date"));
                alarm.setAlarm_description(rs.getString("alarm_description"));
                alarm.setArchive(rs.getInt("archive"));
                alarm.setBlue_alarm(rs.getInt("blue_alarm"));
                alarm.setPump_id(rs.getInt("pump_id"));
                alarm.setTank_id(rs.getInt("tank_id"));
                alarm.setTime_accepted(rs.getString("time_accepted"));
                alarm.setTime_retrieved(rs.getString("time_retrieved"));
                alarm.setValve_id(rs.getInt("valve_id"));
                if (rs.getInt("archive") == 1) {
                    alarm.setColor(4); // Grey Alarm
                }
                if (rs.getInt("alarm_active") == 1 && rs.getInt("acknowledged") == 0) {
                    alarm.setColor(1);// Red Alarm
                }
                if (rs.getInt("alarm_active") == 1 && rs.getInt("acknowledged") == 1) {
                    alarm.setColor(2); // Orange Alarm
                }
                if (rs.getInt("blue_alarm") == 1) {
                    alarm.setColor(3); // Blue Alarm
                }
                alarmList.add(alarm);

            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            return executor.submit(() -> {
                return alarmList;
            });
        } // End of catch
        return executor.submit(() -> {
            return alarmList;
        });

    }//Get  a list of latest 100 alarms from MySql. ******************** Get  a list of latest 100 alarms from MySql. ****************************************************

    //Get tanks table from MySql. ******************** Get tanks table from MySql. ****************************************************
    public Future<List<Tank>> getTanksTable() {
        List<Tank> tankTable = new ArrayList<>();
        try (Connection conn = MySQLJDBCUtil.getConnection()) {
            String query = "select t.tank_id,t.code_name,t.tank_level,t.volume,t.volume_percent,t.weight,t.density,t.low_alarm_limit,t.high_alarm_limit,\n" +
                    "    t.low_low_alarm_limit,t.high_high_alarm_limit,t.alarm_name\n" +
                    "    from   tanks t";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                Tank tank = new Tank();
                tank.setTank_id(rs.getInt("tank_id"));
                tank.setCode_name(rs.getString("code_name"));
                tank.setTank_level(rs.getFloat("tank_level"));
                tank.setVolume(rs.getFloat("volume"));
                tank.setVolume_percent(rs.getFloat("volume_percent"));
                tank.setWeight(rs.getFloat("weight"));
                tank.setDensity(rs.getFloat("density"));
                tank.setLow_alarm_limit(rs.getFloat("low_alarm_limit"));
                tank.setHigh_alarm_limit(rs.getFloat("high_alarm_limit"));
                tank.setLow_low_alarm_limit(rs.getFloat("low_low_alarm_limit"));
                tank.setHigh_high_alarm_limit(rs.getFloat("high_high_alarm_limit"));
                tank.setAlarm_name(rs.getString("alarm_name"));
                tankTable.add(tank);

            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            return executor.submit(() -> {
                return tankTable;
            });
        } // End of catch
        return executor.submit(() -> {
            return tankTable;
        });

    }//Get tanks table from MySql. ******************** Get tanks table from MySql. ****************************************************






    // update Tanks Low limit and High limit ***** update Tanks Low limit and High limit
    public Future<Boolean> updateTankLowAndHighLimit(LevelPostObject levelPostObject) {
        if (Db.tankMapData.containsKey(levelPostObject.getTank_id())) {
            TankDataForMap tankDataForMap = Db.tankMapData.get(levelPostObject.getTank_id());
            tankDataForMap.setTankLowLevel(levelPostObject.getLow_alarm_limit());
            tankDataForMap.setTankHighLevel(levelPostObject.getHigh_alarm_limit());

            try (Connection conn = MySQLJDBCUtil.getConnection()) {

                String updateTanksVolumeH = "UPDATE tanks set high_alarm_limit = ?,low_alarm_limit = ? where tank_id = ? ;";
                PreparedStatement preparedStmt = conn.prepareStatement(updateTanksVolumeH, Statement.RETURN_GENERATED_KEYS);
                preparedStmt.setFloat(1, levelPostObject.getHigh_alarm_limit());
                preparedStmt.setFloat(2, levelPostObject.getLow_alarm_limit());
                preparedStmt.setInt(3, tankDataForMap.getTank_id());
                int rowAffected = preparedStmt.executeUpdate();
                System.out.println("Tank's Low limit and High limit has been updated");
                return executor.submit(() -> {
                    return true;
                });

            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                return executor.submit(() -> {
                    return false;
                });
            }

        } else {
            return executor.submit(() -> {
                return false;
            });
        } // else

    } // Update Tanks Low limit and High limit ***** update Tanks Low limit and High limit

    // Update Tanks Density ***** Update Tanks Density   **************  Update Tanks Density
    public Future<Boolean> updateTankDensity(LevelPostObject levelPostObject) {
        if (Db.tankMapData.containsKey(levelPostObject.getTank_id())) {
            TankDataForMap tankDataForMap = Db.tankMapData.get(levelPostObject.getTank_id());
            tankDataForMap.setDensity(levelPostObject.getDensity());

            try (Connection conn = MySQLJDBCUtil.getConnection()) {

                String updateTanksVolumeH = "UPDATE tanks set density = ? where tank_id = ? ;";
                PreparedStatement preparedStmt = conn.prepareStatement(updateTanksVolumeH, Statement.RETURN_GENERATED_KEYS);
                preparedStmt.setFloat(1, levelPostObject.getDensity());
                preparedStmt.setInt(2, tankDataForMap.getTank_id());
                int rowAffected = preparedStmt.executeUpdate();
                System.out.println("Tank's Density has been updated");
                return executor.submit(() -> {
                    return true;
                });

            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                return executor.submit(() -> {
                    return false;
                });
            }

        } else {
            return executor.submit(() -> {
                return false;
            });
        } // else

    } // Update Tanks Density ***** Update Tanks Density   **************  Update Tanks Density

    // Set temperature limit  ******** Set temperature limit   **************  Set temperature limit
    public Future<Boolean> setTemperatureLimit(LevelPostObject levelPostObject) {
        if (Db.tankMapData.containsKey(levelPostObject.getTank_id())) {
            TankDataForMap tankDataForMap = Db.tankMapData.get(levelPostObject.getTank_id());
            tankDataForMap.setTemperature_limit(levelPostObject.getTemp_limit());

            try (Connection conn = MySQLJDBCUtil.getConnection()) {

                String updateTanksVolumeH = "UPDATE tanks set temperature_limit = ? where tank_id = ? ;";
                PreparedStatement preparedStmt = conn.prepareStatement(updateTanksVolumeH, Statement.RETURN_GENERATED_KEYS);
                preparedStmt.setFloat(1, levelPostObject.getTemp_limit());
                preparedStmt.setInt(2, tankDataForMap.getTank_id());
                int rowAffected = preparedStmt.executeUpdate();
                System.out.println("Tank's Temperature limit has been updated");
                return executor.submit(() -> {
                    return true;
                });

            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                return executor.submit(() -> {
                    return false;
                });
            }
        } else {
            return executor.submit(() -> {
                return false;
            });
        } // else

    } // Set temperature limit  ******** Set temperature limit   **************  Set temperature limit

    //Get  temperature limit. ******************** Get  temperature limit. ****************************************************
    public Future<Integer> getTemperatureLimit(int tank_id) {
        int temperatureLimit = 0;
        try (Connection conn = MySQLJDBCUtil.getConnection()) {
            String query = "select t.temperature_limit  from  tanks t WHERE tank_id = ?";
            PreparedStatement preparedStmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStmt.setInt(1, tank_id);
            ResultSet rs = preparedStmt.executeQuery();

            while (rs.next()) {
                temperatureLimit = rs.getInt("temperature_limit");

            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            int finalTemperatureLimit = temperatureLimit;
            return executor.submit(() -> {
                return finalTemperatureLimit;
            });
        } // End of catch
        int finalTemperatureLimit1 = temperatureLimit;
        return executor.submit(() -> {
            return finalTemperatureLimit1;
        });

    }//Get  temperature limit. ******************** Get  temperature limit. ****************************************************


} // Class
