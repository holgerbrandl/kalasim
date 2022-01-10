# Documentation Build

Kalasim documentation is build with [mkdocs](https://www.mkdocs.org/).

```bash
#pip install mkdocs
#pip install mkdocs-material
cd  /c/brandl_data/projects/scheduling/kalasim/docs/userguide

#pip install markdown-include
#pip install pymdown-extensions # not needed  

# workaround for  https://github.com/mkdocs/mkdocs/issues/2469
#pip install -Iv importlib_metadata==4.5.0

#mkdocs new .

mkdocs serve
mkdocs serve

mkdocs build
```

For more details see <https://squidfunk.github.io/mkdocs-material/creating-your-site/>


## Tech Pointers

For publishing options see <https://squidfunk.github.io/mkdocs-material/publishing-your-site/>

Nice options overview <https://github.com/squidfunk/mkdocs-material/blob/master/mkdocs.yml>

include code into mkdocs  <https://github.com/mkdocs/mkdocs/issues/777> <https://github.com/cmacmackin/markdown-include>

header stripping ? Not yet, see <https://github.com/cmacmackin/markdown-include/issues/9>

**{todo}** consider using snippets <https://squidfunk.github.io/mkdocs-material/reference/code-blocks/#snippets>


## Charts with Mermaid

mermaid is 10x more popular than plantuml on github

* comparison <https://ruleoftech.com/2018/generating-documentation-as-code-with-mermaid-and-plantuml>
* <https://www.npmtrends.com/mermaid-vs-plantuml-encoder-vs-jointjs-vs-mxgraph>

Nice examples --> <https://github.com/mermaid-js/mermaid>

Online Editor <https://mermaid-js.github.io/mermaid-live-editor>


## How to build markdown from jupyter notebooks?

```bash
export KALASIM_HOME=/d/projects/scheduling/kalasim
cd ${KALASIM_HOME}/docs/userguide/docs/examples

# clean up SNAPSHOT builds in local ivy cache
# rm -rf ~/.ivy2/cache/com.github.holgerbrandl/kravis
# rm -rf ~/.ivy2/cache/org.kalasim/
 
# cmd.exe "/K" C:\Users\brandl\Anaconda3\Scripts\activate.bat C:\Users\brandl\Anaconda3

## start jupyter without a specific notebook
#jupyter notebook --kernel=kotlin 


## build markdown from notebook

## Bridge-Game
jupyter nbconvert --kernel=kotlin --to markdown bridge_game.ipynb  --out  bridge_game.md


## ATM
jupyter nbconvert --kernel=kotlin --execute  atm_queue.ipynb  --to notebook --inplace
jupyter nbconvert --kernel=kotlin --to markdown  atm_queue.ipynb --out  atm_queue.md

## Gasstation
jupyter nbconvert --kernel=kotlin --to markdown  gas_station.ipynb --out  gas_station.md



cd ${KALASIM_HOME}/docs/userguide/docs/animation

jupyter nbconvert --kernel=kotlin --to markdown  lunar_mining.ipynb --out  lunar_mining.md


```

## Atom Feed

https://ravenreader.app/

https://www.uuidgenerator.net/


https://validator.w3.org/feed/docs/atom.html

https://github.com/PDOK/atom-generator