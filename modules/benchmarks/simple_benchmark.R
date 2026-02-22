#' ---
#' title: "Kalasim Performance Regressionn"
#' format:
#'   html:
#'     toc: true
#'     toc-depth: 3
#'     toc-title: "Contents"
#'     toc-location: left
#'     self-contained: true
#' lightbox: true
#' fig.width: 12
#' max-width: 90%
#' engine: knitr
#' ---

#' Load Packages

pacman::p_load(tidyverse, scales, grid, magrittr, readxl, digest, snakecase, lubridate)

pacman::p_load_gh("holgerbrandl/datautils")


scoreMetrics = list.files(".", "giga4d.*.csv") %>%
  map(~{
    read_csv(.x, show_col_types = FALSE) %>%
      mutate(
        ticktime = str_split_fixed(.x, "[_]", 3)[, 2] %>% ymd,
        build = str_split_fixed(.x, "[._]", 4)[, 3]
      )
  }) %>%
  bind_rows %>%
  # filter(str_detect(build, "lc-")) %>%
  pretty_columns


scoreMetrics %>%
  # filter(str_detect(build, "nu-")) %>%
  ggplot(aes(reorder(build, timestamp), runtime)) +
  geom_boxplot() +
  geom_jitter(alpha = 0.7) +
  ggtitle("Performance Progression") +
  scale_y_continuous(limits = c(0, NA))

