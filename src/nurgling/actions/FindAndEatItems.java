package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.areas.NContext;
import nurgling.iteminfo.NFoodInfo;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class FindAndEatItems implements Action
{
    final NContext cnt;
    ArrayList<String> items;
    double level;
    Pair<Coord2d,Coord2d> area;
    public FindAndEatItems(NContext context, ArrayList<String> items, int level, Pair<Coord2d,Coord2d> area)
    {
        this.cnt = context;
        this.items = items;
        this.level = level;
        this.area = area;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        for(String item: items)
        {
           cnt.addInItem(item, null);
        }

        if (area != null) {
            for (Gob pile : Finder.findGobs(area, new NAlias("stockpile"))) {
                if (!calcCalories())
                    break;
                takeFromPile(gui, pile);
            }
            for (Gob contgob : Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet()), new ArrayList<>()))) {
                if (!calcCalories())
                    break;
                takeFromContainer(gui, contgob);
            }
        }
        eatAll(gui);
        return Results.SUCCESS();
    }

    public Results takeFromPile(NGameUI gui, Gob pile) throws InterruptedException
    {
        new PathFinder(pile).run(gui);
        new OpenTargetContainer("Stockpile",  pile).run(gui);
        while (calcCalories()) {
            if(gui.getInventory().getNumberFreeCoord(new Coord(1,1))==0)
            {
                eatAll(gui);
            }
            TakeItemsFromPile tifp;
            (tifp = new TakeItemsFromPile(pile, gui.getStockpile(), 1)).run(gui);
            if(tifp.getResult() == 0)
                break;
        }
        new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
        return Results.SUCCESS();
    }

    public Results takeFromContainer(NGameUI gui, Gob contgob) throws InterruptedException
    {
        Container cont = new Container(contgob, NContext.contcaps.get(contgob.ngob.name), null);
        new PathFinder(contgob).run(gui);
        new OpenTargetContainer(cont).run(gui);
        while (calcCalories()) {
            if(gui.getInventory().getNumberFreeCoord(new Coord(1,1))==0)
            {
                eatAll(gui);
            }
            WItem taritem = NUtils.getGameUI().getInventory(cont.cap).getItem(new NAlias(items));
            int oldSize = NUtils.getGameUI().getInventory().getItems(new NAlias(items)).size();
            if( taritem == null )
                break;
            taritem.item.wdgmsg("transfer", Coord.z);
            gui.ui.core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), new NAlias(items), oldSize + 1));
        }

        new CloseTargetContainer(cont).run(gui);
        return Results.SUCCESS();
    }

    boolean calcCalories() throws InterruptedException {
        double curlvl = NUtils.getEnergy()*10000;
        ArrayList<WItem> taritems = NUtils.getGameUI().getInventory().getItems(new NAlias(items));
        for(WItem item: taritems)
        {
            NFoodInfo fi = ((NGItem)item.item).getInfo(NFoodInfo.class);
            curlvl+=fi.end*100;
        }
        return curlvl<level;
    }

    void eatAll(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> titems = NUtils.getGameUI().getInventory().getItems(new NAlias(items));

        for (WItem item : titems)
        {
            new SelectFlowerAction("Eat", (NWItem) item).run(gui);
        }
    }
}
