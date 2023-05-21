#' Kalasim Benchmark
#'
# cd /Users/brandl/projects/kotlin/krangl/examples/benchmarking; R
pacman::p_load(tidyverse, scales, grid, magrittr, readxl, digest, snakecase, lubridate)
pacman::p_load_gh("holgerbrandl/datautils")

# pacman::p_load_gh(jsonlite)


ex ="perf_logs/jmh_results_20230522T202200.csv"
basename(ex) %>% {str_split_fixed(., fixed("_"), 3)[,3]} %>% trim_ext(".csv")


# https://stackoverflow.com/questions/23089895/how-to-remove-time-field-string-from-a-date-as-character-variable
parse_timestamp = function(x)
str_replace(x, "T", " ") |> parse_date_time("%Y%m%d %H%M%S")

# perfData = "perf_logs/benchmarks.csv" %>%
perfData = list.files("perf_logs", "jmh_results_*", full = T) %>%
  map(~{
    read_csv(.x,show_col_types = FALSE) %>%
      mutate(
        run = basename(.x) %>% {str_split_fixed(., fixed("_"), 3)[,3]} %>% trim_ext(".csv") %>% parse_timestamp
      )
  }) %>%
  bind_rows %>%
    pretty_columns() %>%
    rename(ci = `score_error_99_9`)

perfData %>% mutate()

perfData %>%
    filter(mode == "avgt") %>%
    filter(benchmark == "org.kalasim.benchmarks.BackendBench.columnArithmetics") %>%
    ggplot(aes(as.factor(param_k_rows), score)) +
    geom_col() +
    geom_errorbar(aes(ymin = score - ci, ymax = score + ci), width = .2) +
    ggtitle("column arithmetics") #+ scale_x_log10()
    # coord_cartesian(ylim=c(0, 5))


## by using json we would have better access to raw data
# jsonData = fromJSON("perf_logs/benchmarks.csv")
# results = flatten(jsonData) %>% transmute(benchmark, runtime = primaryMetric.score, ci = primaryMetric.scoreError)
# results$benchmark %<>% str_match("Benchmark.(.*)") %>% get_col(2)
#
# # note 1% CI is used here
# results %>% ggplot(aes(benchmark, runtime)) +
#     geom_bar(stat = "identity") +
#     geom_errorbar(aes(ymin = runtime - ci, ymax = runtime + ci), width = .2)