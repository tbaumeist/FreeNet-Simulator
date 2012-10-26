#/bin/bash
#Exit if any command exits with an error
set -o errexit

#Start from directory in which the script is located.
cd `dirname $0`
startingDir=`pwd`

###Configure###
#Location of the routing simulator
jar="$startingDir/target/routing_simulator-0.0.1-dev-jar-with-dependencies.jar"
#Path to pyProbe installation (for plotting gnuplot scripts)
gnuplotDir="$startingDir"
#General network size
size=18000
#Ideal graph settings
ideal="--ideal --local 0 --remote 6"
#Degree graph settings
degree="--degree $startingDir/peerDist.dat --link $startingDir/links_output_deduped --force-size"
#Simulation settings
simulation="--probe 50"

###End configure###

#TODO: Some way to not repeat directory names, or generate them from simulation parameters. Perhaps Python would be better-suited?

#make the folder
mkdir -p "degree_mh_$size"

echo $degree --size "$size" --metropolis-hastings -D "degree_mh_$size/peerDist.dat" -L "degree_mh_$size/links_output" -O "degree_mh_$size/occurenceDistribution/" $simulation > test.config
#echo $degree --size "$size" -D "degree_uniform_$size/peerDist.dat" -L "degree_uniform_$size/links_output" -O "degree_uniform_$size/occurenceDistribution/" $simulation >> test.config
#echo $ideal --size "$size" --metropolis-hastings -D "ideal_mh_$size/peerDist.dat" -L "ideal_mh_$size/links_output" -O "ideal_mh_$size/occurenceDistribution/" $simulation >> test.config
#echo $ideal --size "$size" -D "ideal_uniform_$size/peerDist.dat" -L "ideal_uniform_$size/links_output" -O "ideal_uniform_$size/occurenceDistribution/" $simulation >> test.config

#TODO: This breaks on paths with spaces because -C uses a space as a column separator for different arguments.
#Uses http://www.gnu.org/software/parallel/
parallel -C " " java -jar "$jar" < test.config

for dir in "degree_mh_$size" "degree_uniform_$size" "ideal_mh_$size" "ideal_uniform_$size"
do
    cd "$dir"
    #Link length distribution
    gnuplot "$gnuplotDir"/link_length.gnu
    #Degree distribution
    gnuplot "$gnuplotDir"/peer_dist.gnu
    gnuplot "$startingDir"/probeDistribution.gnu
    cd "$startingDir"
done
