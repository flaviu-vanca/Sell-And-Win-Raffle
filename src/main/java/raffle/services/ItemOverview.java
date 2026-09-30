package raffle.services;

import raffle.models.Item;

/** An item with its sales figures, as the main screen lists it. */
public record ItemOverview(Item item, ItemSales sales) {
}
