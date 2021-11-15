```bash
export KALASIM_HOME=/d/projects/scheduling/kalasim
cd ${KALASIM_HOME}/simulations/notebooks/

# clean up SNAPSHOT builds in local ivy cache
# rm -rf ~/.ivy2/cache/com.github.holgerbrandl/kravis
# rm -rf ~/.ivy2/cache/org.kalasim/
 
#https://stackoverflow.com/questions/35254852/how-to-change-the-jupyter-start-up-folder
#cd projects\scheduling\kalasim\simulations\notebooks 

# cmd.exe "/K" C:\Users\brandl\Anaconda3\Scripts\activate.bat C:\Users\brandl\Anaconda3



## start jupyter without a specific notebook
jupyter notebook --kernel=kotlin 

#conda install -c jetbrains kotlin-jupyter-kernel
jupyter notebook --kernel=kotlin dining.ipynb

 
## start with notebook
jupyter notebook --kernel=kotlin hospital.ipynb


## build markdown from notebook

jupyter nbconvert  --to html bridge_game.ipynb  # 

cd D:\projects\scheduling\kalasim\docs\userguide\docs\examples
#jupyter nbconvert --kernel=kotlin --execute --to markdown  bridge_game.ipynb  --out  bridge_game.md
jupyter nbconvert --kernel=kotlin --to markdown  bridge_game.ipynb  --out  bridge_game.md

```