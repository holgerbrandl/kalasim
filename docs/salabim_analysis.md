* customers -->yield passivate --> should be passive and no longer scheduled according to https://www.salabim.org/manual/Component.html#
* terminate also does not change schedule but simply sets scheduled to infinity

**{todo}** will infinity scheduled items ever be run?


### heapq notes


https://docs.python.org/3/library/heapq.html


> Heap elements can be tuples. This is useful for assigning comparison values (such as task priorities) alongside the main record being tracked:


Comparator support on types is possible (see https://www.bogotobogo.com/python/python_PriorityQueue_heapq_Data_Structure.php)


As pointed out in `salabim.Environment.run` event list is sorted first via time, then via prio, and finally via urgency --> How is this reflected in the implemenation?

heapq does sort even by payload if prio sorting results in a tie https://docs.python.org/3/library/heapq.html#priority-queue-implementation-notes

From https://stackoverflow.com/questions/60305637/does-heapq-heappush-compare-on-int-and-string-without-been-specified-for
> heapq just compares values from the queue using using the "less than"

```
>>> (0, 'a') < (1, 'aa')
True
>>> (1, 'a') < (1, 'aa')
True
>>> (1, 'aa') < (1, 'a')
False
>>> (2, 'a') < (1, 'aa')
False
```

If the first element of each tuple is the same, the comparisons are determined solely by the second, and so on