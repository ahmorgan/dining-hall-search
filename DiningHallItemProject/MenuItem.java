public class MenuItem {
    private String name;
    private String date;
    private String station;
    private String meal;
    private String diningHall;

    public MenuItem(String name, String date, String station, String meal, String diningHall) {
        this.name = name;
        this.date = date;
        this.station = station;
        this.meal = meal;
        this.diningHall = diningHall;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public String getStation() {
        return station;
    }

    public String getMeal() {
        return meal;
    }

    public String getDiningHall() {
        return diningHall;
    }

    public String toString() {
        return name + " will next be served on... " + date + " ...for meal period... " + meal + " ...at... " + diningHall + " ...at station... " + station;
    }
}
