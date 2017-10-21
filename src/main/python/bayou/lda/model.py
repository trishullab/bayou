# Copyright 2017 Rice University
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import numpy as np
import pickle
from collections import OrderedDict

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.decomposition import LatentDirichletAllocation


class LDA():

    def __init__(self, args=None, from_file=None):
        # Initialize LDA model from either arguments or a file. If both are
        # provided, file will be used.
        assert args or from_file, 'Improper initialization of LDA model'
        if from_file is not None:
            with open(from_file, 'rb') as f:
                self.model, self.vectorizer = pickle.load(f, encoding='latin1')
        else:
            self.vectorizer = TfidfVectorizer(lowercase=False, token_pattern=u'[^;]+')
            alpha = args.alpha if args.alpha is not None else 50./args.ntopics
            beta = args.beta if args.beta is not None else 200./len(self.vectorizer.vocabulary_)
            print('{} words in vocabulary'.format(len(self.vectorizer.vocabulary_)))
            print('Training LDA with {} topics, {} alpha, {} beta'.format(args.ntopics, alpha, beta))
            self.model = LatentDirichletAllocation(args.ntopics,
                                                   doc_topic_prior=alpha, topic_word_prior=beta,
                                                   learning_method='batch', max_iter=100,
                                                   verbose=1, evaluate_every=1,
                                                   max_doc_update_iter=100, mean_change_tol=1e-5)

    def top_words(self, n):
        features = self.vectorizer.get_feature_names()
        words = [OrderedDict([(features[i], topic[i]) for i in topic.argsort()[:-n - 1:-1]])
                 for topic in self.model.components_]
        return words

    def train(self, docs):
        data = [';'.join(bow) for bow in docs]
        vect = self.vectorizer.fit_transform(data)
        self.model.fit(vect)
        # normalizing does not change subsequent inference, provided no further training is done
        self.model.components_ /= self.model.components_.sum(axis=1)[:, np.newaxis]

    def infer(self, docs):
        data = [';'.join(bow) for bow in docs]
        vect = self.vectorizer.transform(data)
        dist = self.model.transform(vect)
        assert vect.shape[0] == dist.shape[0]

        # NOTE: if a document is empty, this method returns a zero topic-dist vector
        samples = [list(doc_topic_dist) if m.nnz > 0 else ([0.] * self.model.n_components)
                   for m, doc_topic_dist in zip(vect, dist)]
        return samples

