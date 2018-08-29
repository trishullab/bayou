# start with Ubuntu 16.04 base
FROM ubuntu:16.04
RUN apt-get update && apt-get install -y apt-utils git

# clone Bayou
RUN git clone https://github.com/capergroup/bayou.git ~/bayou

# install build dependencies
RUN (cd ~/bayou/tool_files/build_scripts; ./install_dependencies.sh)

# build Bayou
RUN (cd ~/bayou/tool_files/build_scripts; ./build.sh)

# install deployment dependencies
RUN (cd ~/bayou/tool_files/build_scripts/out; ./install_dependencies.sh)

# make start_bayou.sh the entry point
ENTRYPOINT ~/bayou/tool_files/build_scripts/out/start_bayou.sh
