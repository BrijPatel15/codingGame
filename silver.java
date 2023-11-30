import java.util.*;
import java.util.stream.Collectors;

// Define the data structures as records
record Vector(int x, int y) {}

record FishDetail(int color, int type) {}

record Fish(int fishId, Vector pos, Vector speed, FishDetail detail) {}

record Drone(int droneId, Vector pos, boolean dead, int battery, List<Integer> scans) {}

record RadarBlip(int fishId, String dir) {}

class Player {
    static Map<Integer, String> nextMoves = new HashMap<>();
    private static void queueNextMove(int droneId, String command){
        System.err.println("DRONEID " + droneId + " QUEUED: " + command);
        nextMoves.putIfAbsent(droneId, command);
    }

    private static void nextMove(){
        for(String move : nextMoves.values()){
            System.err.println("MOVE " + move);
            System.out.println(move);
        }

        nextMoves.clear();
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        Map<Integer, FishDetail> fishDetails = new HashMap<>();
        // Get all the fish that are in the ocean
        int fishCount = in.nextInt();
        for (int i = 0; i < fishCount; i++) {
            int fishId = in.nextInt();
            int color = in.nextInt();
            int type = in.nextInt();
            fishDetails.put(fishId, new FishDetail(color, type));
        }

        // game loop
        while (true) {
            List<Integer> myScans = new ArrayList<>();
            List<Integer> foeScans = new ArrayList<>();
            Map<Integer, Drone> droneById = new HashMap<>();
            List<Drone> myDrones = new ArrayList<>();
            List<Drone> foeDrones = new ArrayList<>();
            List<Fish> visibleFishes = new ArrayList<>();
            Map<Integer, List<RadarBlip>> myRadarBlips = new HashMap<>();

            //score is the total num of points we've earned so far
            int myScore = in.nextInt();
            int foeScore = in.nextInt();
            
            //scanCount is the number of fish you've scanned in your current dive
            int myScanCount = in.nextInt();
            for (int i = 0; i < myScanCount; i++) {
                int fishId = in.nextInt();
                myScans.add(fishId);
            }

            int foeScanCount = in.nextInt();
            for (int i = 0; i < foeScanCount; i++) {
                int fishId = in.nextInt();
                foeScans.add(fishId);
            }

            
            //multiple drones 
            int myDroneCount = in.nextInt();
            for (int i = 0; i < myDroneCount; i++) {
                int droneId = in.nextInt();
                int droneX = in.nextInt();
                int droneY = in.nextInt();
                boolean dead = in.nextInt() == 1;
                int battery = in.nextInt();
                Vector pos = new Vector(droneX, droneY);
                Drone drone = new Drone(droneId, pos, dead, battery, new ArrayList<>());
                droneById.put(droneId, drone);
                myDrones.add(drone);
                myRadarBlips.put(droneId, new ArrayList<>());
            }

            int foeDroneCount = in.nextInt();
            for (int i = 0; i < foeDroneCount; i++) {
                int droneId = in.nextInt();
                int droneX = in.nextInt();
                int droneY = in.nextInt();
                boolean dead = in.nextInt() == 1;
                int battery = in.nextInt();
                Vector pos = new Vector(droneX, droneY);
                Drone drone = new Drone(droneId, pos, dead, battery, new ArrayList<>());
                droneById.put(droneId, drone);
                foeDrones.add(drone);
            }
            
            //each drone has its own scan count and this before they go up and "save" the points 
            int droneScanCount = in.nextInt();
            for (int i = 0; i < droneScanCount; i++) {
                int droneId = in.nextInt();
                int fishId = in.nextInt();
                droneById.get(droneId).scans().add(fishId);
            }
            
            int visibleFishCount = in.nextInt();
            for (int i = 0; i < visibleFishCount; i++) {
                int fishId = in.nextInt();
                int fishX = in.nextInt();
                int fishY = in.nextInt();
                int fishVx = in.nextInt();
                int fishVy = in.nextInt();
                Vector pos = new Vector(fishX, fishY);
                Vector speed = new Vector(fishVx, fishVy);
                FishDetail detail = fishDetails.get(fishId);
                visibleFishes.add(new Fish(fishId, pos, speed, detail));
            }

            int myRadarBlipCount = in.nextInt();
            for (int i = 0; i < myRadarBlipCount; i++) {
                int droneId = in.nextInt();
                int fishId = in.nextInt();
                String radar = in.next();
                myRadarBlips.get(droneId).add(new RadarBlip(fishId, radar));
            }

            for (Drone drone : myDrones) {
                System.err.println(drone.droneId() + " scanCount: " + myScanCount + " droneScanCount: " + droneScanCount+ " visibleFishCount:" 
            + visibleFishCount + " myRadarBlipCount: " + myRadarBlipCount);
                //save scans
                if(drone.battery() <= 5 || drone.scans().size() >= 5){
                    // System.out.println("MOVE " + drone.pos().x() + " 500 0 SURFACE, BATTERY " + drone.battery());
                    queueNextMove(drone.droneId(), "MOVE " + drone.pos().x() + " 500 0 SURFACE, BATTERY " + drone.battery());
                } else {
                    Fish target = null;
                    double minDist = Double.MAX_VALUE;
                    //double maxValue = Double.MIN_VALUE;
                    System.err.println("Trying to find visible fish");
                    //Find a fish that is visible IDEA: maybe we can auto sort visibleFishes by distance
                    for(Fish fish : visibleFishes){
                        if(!drone.scans().contains(fish.fishId()) && !myScans.contains(fish.fishId())){
                            System.err.println("Going to visible fish");
                            if((drone.droneId() == 0 && drone.pos().x() + 500 > 5000) || (drone.droneId() == 2 && drone.pos().x() - 500 <= 5000)){
                                break;
                            }
                            double dist = Math.hypot(drone.pos().x() - fish.pos().x(), drone.pos().y() - fish.pos().y());
                            if (dist < minDist) {
                                minDist = dist;
                                target = fish;
                            }
                        }
                    }

                    if(target != null){
                        Vector predictedFishPos = new Vector(target.pos().x() + target.speed().x(), target.pos().y() + target.speed().y());
                        // System.out.println("MOVE " + predictedFishPos.x() + " " + predictedFishPos.y() + " 1 VISIBLE BAT " + drone.battery());
                        queueNextMove(drone.droneId(), "MOVE " + predictedFishPos.x() + " " + predictedFishPos.y() + " 1 VISIBLE BAT " + drone.battery());
                    // We havent found any visible fishes time to rely on the radar
                    } else {
                        List<RadarBlip> blips = myRadarBlips.get(drone.droneId());
                        System.err.println("Visible fish not found going to blip");
                        RadarBlip targetBlip = null;

                        int targetX = drone.pos().x();
                        int targetY = drone.pos().y();
                        int stepSize = 500;

                        List<RadarBlip> test = blips.stream().filter((b) -> {
                            final List<String> drone0BadList = Arrays.asList("TR", "BR");
                            final List<String> drone2BadList = Arrays.asList("TL", "BL");

                            if (drone.droneId() == 0 && drone.pos().x() + stepSize > 5000 && drone0BadList.contains(b.dir())) {
                                return false;
                            }

                            if (drone.droneId() == 2 && drone.pos().x() - stepSize <= 5000 && drone2BadList.contains(b.dir())) {
                                return false;
                            }

                            return true;
                        }).collect(Collectors.toList());
                        System.err.println(drone.droneId() + " OG BLIP count: " + blips.size() + " N BLIP: " + test.size());

                        for(RadarBlip b : test){
                            if(!drone.scans().contains(b.fishId()) && !myScans.contains(b.fishId())){
                                targetBlip = b;
                                break;
                            }
                        }

                        if(targetBlip != null){
                            switch(targetBlip.dir()){
                            case "TL":
                                targetY -= stepSize;
                                targetX -= stepSize;
                                break;
                            case "TR":
                                targetY -= stepSize;
                                targetX += stepSize;
                                break;
                            case "BL":
                                targetY += stepSize;
                                targetX -= stepSize;
                                break; 
                            case "BR":
                                targetY += stepSize;
                                targetX += stepSize;
                                break;
                            }
                            // System.out.println("MOVE " + targetX + " " + targetY + " 0 BLIP " + targetBlip.dir()+ " BAT " + drone.battery());
                            if ((drone.droneId() == 0 && targetX <= 5000) || (drone.droneId() == 2 && targetX > 5000)) {
                                // Move the drone to the target position
                                // System.out.println("MOVE " + targetX + " " + targetY + " 0");
                                queueNextMove(drone.droneId(), "MOVE " + targetX + " " + targetY + " 0");
                            }
                        } else {
                            // System.out.println("MOVE " + drone.pos().x() + " 500 0 SURFACE, BATTERY " + drone.battery());
                            queueNextMove(drone.droneId(), "MOVE " + drone.pos().x() + " 500 0 SURFACE, BATTERY " + drone.battery());
                        }
                        
                    }
                }
            }
            nextMove();
        }
    }
}
