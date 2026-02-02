package fr.xamez.moka.tool;

import java.util.ArrayList;
import java.util.List;

public class MokaMenuBar {
    private final List<Menu> menus = new ArrayList<>();

    public static MokaMenuBar create() {
        return new MokaMenuBar();
    }

    public MokaMenuBar add(Menu menu) {
        menus.add(menu);
        return this;
    }

    public List<Menu> getMenus() {
        return menus;
    }

    public interface Item {
    }

    public static class Menu implements Item {
        private final String label;
        private final List<Item> items = new ArrayList<>();

        public static Menu create(String label) {
            return new Menu(label);
        }

        public Menu(String label) {
            this.label = label;
        }

        public Menu add(Item item) {
            items.add(item);
            return this;
        }

        public Menu item(String label, Runnable action) {
            items.add(new Action(label, action));
            return this;
        }

        public Menu submenu(Menu menu) {
            items.add(menu);
            return this;
        }

        public Menu separator() {
            items.add(new Separator());
            return this;
        }

        public String getLabel() {
            return label;
        }

        public List<Item> getItems() {
            return items;
        }
    }

    public record Action(String label, Runnable callback) implements Item {
    }

    public static class Separator implements Item {
    }
}