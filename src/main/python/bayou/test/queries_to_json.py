import json
import sys

queries_file = sys.argv[1]

with open(queries_file) as f:
    lines = f.readlines()
    queries = [line.strip() for line in lines]

javadoc_instances = []
js = {'programs': javadoc_instances}
for query in queries:
    javadoc_instances.append({'javadoc': query})

with open(queries_file+'.json', 'w') as f:
    json.dump(js, f, indent=2)
