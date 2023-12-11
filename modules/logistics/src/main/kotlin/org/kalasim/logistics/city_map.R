require(tidyverse)

setwd("D:/projects/scheduling/kalasim")

segments = read_csv("city_map.segments.csv")
buildings = read_csv("city_map.buildings.csv")

ggplot(segments, aes(from_position_x, from_position_y, xend = to_position_x, yend = to_position_y)) +
  geom_segment(size = 3, color = "azure2") +
  # theme_void() +
  theme_bw() +
  theme(axis.line = element_blank(), axis.text.x = element_blank(),
        axis.text.y = element_blank(), axis.ticks = element_blank(),
        axis.title.x = element_blank(),
        axis.title.y = element_blank(),
        panel.grid.major = element_blank(), panel.grid.minor = element_blank()) +
  # theme(legend.position = "none") +
  geom_point(aes(port_position_x + 2, port_position_y + 1, colour = type, size = type, xend = NULL, yend = NULL), data = buildings, shape = 15) +
  scale_size_manual(values = c("Factory" = 3, "Business" = 2, "Home" = 1)) +
  ggtitle("City Map")