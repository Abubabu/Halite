package hlt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.util.Collection;

public class GameMap {
    private final int width, height;
    private final int playerId;
    private final List<Player> players;
    private final List<Player> playersUnmodifiable;
    private final Map<Integer, Planet> planets;
    private final List<Ship> allShips;
    private final List<Ship> allShipsUnmodifiable;

    // used only during parsing to reduce memory allocations
    private final List<Ship> currentShips = new ArrayList<>();

    public GameMap(final int width, final int height, final int playerId) {
        this.width = width;
        this.height = height;
        this.playerId = playerId;
        players = new ArrayList<>(Constants.MAX_PLAYERS);
        playersUnmodifiable = Collections.unmodifiableList(players);
        planets = new TreeMap<>();
        allShips = new ArrayList<>();
        allShipsUnmodifiable = Collections.unmodifiableList(allShips);
    }
    public void addShip(GhostShip ship)
    {
    	allShips.add(ship);
    }
    public void remove(GhostShip ship)
    {
    	allShips.remove(ship);
    }
    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getMyPlayerId() {
        return playerId;
    }

    public List<Player> getAllPlayers() {
        return playersUnmodifiable;
    }

    public Player getMyPlayer() {
        return getAllPlayers().get(getMyPlayerId());
    }

    public Ship getShip(final int playerId, final int entityId) throws IndexOutOfBoundsException {
        return players.get(playerId).getShip(entityId);
    }
    public Planet getPlanet(final int entityId) {
        return planets.get(entityId);
    }

    public Map<Integer, Planet> getAllPlanets() {
        return planets;
    }

    public List<Ship> getAllShips() {
        return allShipsUnmodifiable;
    }

    public ArrayList<Entity> objectsBetween(Position start, Position target) {
        final ArrayList<Entity> entitiesFound = new ArrayList<>();

        addEntitiesBetween(entitiesFound, start, target, planets.values());
        addEntitiesBetween(entitiesFound, start, target, allShips);

        return entitiesFound;
    }
    public ArrayList<Entity> planetsBetween(Position start, Position target) {
        final ArrayList<Entity> entitiesFound = new ArrayList<>();

        addEntitiesBetween(entitiesFound, start, target, planets.values());
        return entitiesFound;
    }
    public ArrayList<Entity> shipsBetween(Position start, Position target) {
        final ArrayList<Entity> entitiesFound = new ArrayList<>();

        addEntitiesBetween(entitiesFound, start, target, allShips);

        return entitiesFound;
    }
    private static void addEntitiesBetween(final List<Entity> entitiesFound,
                                           final Position start, final Position target,
                                           final Collection<? extends Entity> entitiesToCheck) {

        for (final Entity entity : entitiesToCheck) {
            if (entity.equals(start) || entity.equals(target)) {
                continue;
            }
            if (Collision.vectorIntersect(start, target, entity, Constants.FORECAST_FUDGE_FACTOR)) { 
                entitiesFound.add(entity);
            }
        }
    }
   
    public Map<Double, Entity> nearbyEntitiesByDistance(final Entity entity) {
        final Map<Double, Entity> entityByDistance = new TreeMap<>();

        for (final Planet planet : planets.values()) {
            if (planet.equals(entity)) {
                continue;
            }
            entityByDistance.put(entity.getDistanceTo(planet), planet);
        }

        for (final Ship ship : allShips) {
            if (ship.equals(entity)) {
                continue;
            }
            entityByDistance.put(entity.getDistanceTo(ship), ship);
        }

        return entityByDistance;
    }
   
    public Map<Double, Planet> nearbyPlanetsByDistance(Entity entity) {
        final Map<Double, Planet> planetsByDistance = new TreeMap<>();

        for (final Planet planet : planets.values()) {
            if (planet.equals(entity)) {
                continue;
            }
            planetsByDistance.put(entity.getDistanceTo(planet), planet);
        }


        return planetsByDistance;
    }
    public ArrayList<Planet> nearbyPlanetsByDistanceCollision(Position entity) {
        final ArrayList<Planet> planetsByDistance = new ArrayList<Planet>();

        for (final Planet planet : planets.values()) {
            if (planet.getDistanceTo(entity)<21) {
                planetsByDistance.add(planet);
            }
        }
        return planetsByDistance;
    }
    public ArrayList<Ship> nearbyShipsByDistanceCollision(Position entity) {
        final ArrayList<Ship> shipsByDistance = new ArrayList<Ship>();

        for (final Ship ship : allShips) {
            if (ship.getDistanceTo(entity)<21) {
                shipsByDistance.add(ship);
            }
        }
        return shipsByDistance;
    }
    public Map<Double, Ship> nearbyShipsByDistance(final Entity entity) {
        final Map<Double, Ship> shipsByDistance = new TreeMap<>();
        for (final Ship ship : allShips) {
            if (ship.equals(entity)) {
                continue;
            }
            shipsByDistance.put(entity.getDistanceTo(ship), ship);
        }

        return shipsByDistance;
    }
    public ArrayList<Planet> returnArrayOfPlanets()
    {
    	ArrayList<Planet> yes = new ArrayList<>();
    	for(final Planet planet : planets.values())
    	{
    		yes.add(planet);
    	}
    	return yes;
    }
    public int returnMyPlanets()
    {
    	ArrayList<Planet> yes = new ArrayList<>();
    	for(Planet planet: planets.values())
    	{
    		if(planet.getId() == playerId)
    		{
    			yes.add(planet);
    		}
    	}
    	return yes.size();
    }
    public GameMap updateMap(final Metadata mapMetadata) {
        DebugLog.addLog("--- NEW TURN ---");
        final int numberOfPlayers = MetadataParser.parsePlayerNum(mapMetadata);

        players.clear();
        planets.clear();
        allShips.clear();

        // update players info
        for (int i = 0; i < numberOfPlayers; ++i) {
            currentShips.clear();
            final Map<Integer, Ship> currentPlayerShips = new TreeMap<>();
            final int playerId = MetadataParser.parsePlayerId(mapMetadata);

            final Player currentPlayer = new Player(playerId, currentPlayerShips);
            MetadataParser.populateShipList(currentShips, playerId, mapMetadata);
            allShips.addAll(currentShips);

            for (final Ship ship : currentShips) {
                currentPlayerShips.put(ship.getId(), ship);
            }
            players.add(currentPlayer);
        }

        final int numberOfPlanets = Integer.parseInt(mapMetadata.pop());

        for (int i = 0; i < numberOfPlanets; ++i) {
            final List<Integer> dockedShips = new ArrayList<>();
            final Planet planet = MetadataParser.newPlanetFromMetadata(dockedShips, mapMetadata);
            planets.put(planet.getId(), planet);
        }

        if (!mapMetadata.isEmpty()) {
            throw new IllegalStateException("Failed to parse data from Halite game engine. Please contact maintainers.");
        }

        return this;
    }
}
