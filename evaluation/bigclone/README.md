# Running FaCoY on IJaDataset and evaluating on BigCloneBench (RQ2)

## IJaDataset
IJaDataset is a large java inter-project software repository built from [SeClone project](https://sites.google.com/site/asegsecold/projects/seclone) by Ambient Software Evoluton Group. The current version is IJaDataset-2.0 which is available at the [BigCloneBench page](https://github.com/clonebench/BigCloneBench/blob/master/README.md) otherwise we should ask [SeClone project](https://sites.google.com/site/asegsecold/projects/seclone) site administrator.
IJaDatset is divided into three categories (sample, default, selected) and we used all of them for the experiment.

## BigCloneBench
BigCloneBench is a benchmark for clone detection mined from the IJaDataset-2.0 (25,000 subject systems, 365MLOC). The current version of the benchmark BigCloneBench is available at [BigCloneBench page](https://github.com/clonebench/BigCloneBench/blob/master/README.md). People can see plenty of java snippet clone pairs with various metadata such as functionalities, syntactic types, similarity lines, similarity tokens, projects of each function, etc.

## Analysis of false postives
Although it is one of the highest-quality and largest benchmarks available to the research community, BigCloneBench clone information may not be complete. BigCloneBench is built via an incremental additive process (i.e., gradually relaxing search queries) based on keyword and source pattern matching. Thus, it may miss some clones despite the manual verification. 
Therefore, we manually verify the clone pairs that are not associated in BigCloneBench, but FaCoY recommended as code clones, i.e., false positives. Our objective is then to verify to what extent they are indeed false positives and not misses by BigCloneBench. We sample 10 false positives per clone type category for manual check. For 58 out of 60 cases, it turns out that they are actually true positives. Specifically, BigCloneBench missed four Type-1, one Type-2, two Type-3, and 25 Type-4 clones. In some cases, there is a method call dependency between the false positive and the actual clone. In 26 cases, point to the same file but another location than actual clones. Two are actual false positives.

***FaCoY performs better for functionalities requiring external APIs and libraries than those with pure computation tasks. In addition, our approach’s even can detect clones that BigCloneBench missed with a high probability.**

False positive samples are listed and available [here](/evaluation/bigclone/false_positive_samples)
