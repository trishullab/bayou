import codecs
import os
import collections
import numpy as np
import data_reader

def weighted_pick(weights):
    t = np.cumsum(weights)
    s = np.sum(weights)
    return int(np.searchsorted(t, np.random.rand(1)*s))

class DataLoader():
    def __init__(self, input_file, batch_size, seq_length):
        self.batch_size = batch_size
        self.seq_length = seq_length

        print("reading text file")
        self.preprocess(input_file)
        self.create_batches()
        self.reset_batch_pointer()

    def preprocess(self, input_file):
        paths = data_reader.read_data(input_file)
        data = [word for path in paths for word in path]
        data_nodes, data_edges, data_topics = zip(*data)
        counter = collections.Counter(data_nodes)
        count_pairs = sorted(counter.items(), key=lambda x: -x[1])
        self.chars, _ = zip(*count_pairs)
        self.vocab_size = len(self.chars)
        self.vocab = dict(zip(self.chars, range(len(self.chars))))
        self.tensor = np.array(list(map(self.vocab.get, data_nodes)))
        # bool array representing True:CHILD_EDGE or False:SIBLING_EDGE
        self.edges = np.array([edge == data_reader.CHILD_EDGE for edge in data_edges], dtype=np.bool)
        self.topics = np.array([np.array(t) for t in data_topics])
        self.ntopics = len(self.topics[0])

    def create_batches(self):
        self.num_batches = int(self.tensor.size / (self.batch_size *
                                                   self.seq_length))

        # When the data (tensor) is too small, let's give them a better error message
        if self.num_batches==0:
            assert False, "Not enough data. Make seq_length and batch_size small."

        self.tensor = self.tensor[:self.num_batches * self.batch_size * self.seq_length]
        self.edges = self.edges[:self.num_batches * self.batch_size * self.seq_length]
        self.topics = self.topics[:self.num_batches * self.batch_size * self.seq_length]
        xdata = self.tensor
        edata = self.edges
        tdata = self.topics
        ydata = np.copy(self.tensor)
        ydata[:-1] = xdata[1:]
        ydata[-1] = xdata[0]
        self.x_batches = np.split(xdata.reshape(self.batch_size, -1), self.num_batches, 1)
        self.e_batches = np.split(edata.reshape(self.batch_size, -1), self.num_batches, 1)
        self.t_batches = np.split(tdata.reshape(self.batch_size, -1, self.ntopics), self.num_batches, 1)
        self.y_batches = np.split(ydata.reshape(self.batch_size, -1), self.num_batches, 1)


    def next_batch(self):
        x, e, t, y = [], [], [], self.y_batches[self.pointer]
        for i in range(self.seq_length):
            x.append(np.array([self.x_batches[self.pointer][batch][i] for batch in  
                range(self.batch_size)], dtype=np.int32))
            e.append(np.array([self.e_batches[self.pointer][batch][i] for batch in  
                range(self.batch_size)], dtype=np.bool_))
            t.append(np.array([self.t_batches[self.pointer][batch][i] for batch in  
                range(self.batch_size)], dtype=np.float))
        self.pointer += 1
        return x, e, t, y

    def reset_batch_pointer(self):
        self.pointer = 0
