/**
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package de.tudarmstadt.ukp.experiments.ej.repeatwithcrowdsource.pos;

import static de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase.INCLUDE_PREFIX
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription

import org.apache.uima.analysis_engine.AnalysisEngineDescription
import org.apache.uima.fit.component.NoOpAnnotator
import org.apache.uima.resource.ResourceInitializationException

import de.tudarmstadt.ukp.dkpro.lab.Lab
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension
//import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask.ExecutionPolicy //old
import de.tudarmstadt.ukp.dkpro.lab.task.BatchTask.ExecutionPolicy
import de.tudarmstadt.ukp.dkpro.tc.core.Constants
import de.tudarmstadt.ukp.dkpro.tc.features.length.NrOfTokensUFE
import de.tudarmstadt.ukp.dkpro.tc.features.ngram.LuceneNGramUFE
import de.tudarmstadt.ukp.dkpro.tc.crfsuite.CRFSuiteAdapter
import de.tudarmstadt.ukp.dkpro.tc.crfsuite.CRFSuiteBatchCrossValidationReport
import de.tudarmstadt.ukp.dkpro.tc.crfsuite.CRFSuiteClassificationReport
import de.tudarmstadt.ukp.dkpro.tc.crfsuite.CRFSuiteBatchTrainTestReport
import de.tudarmstadt.ukp.dkpro.tc.crfsuite.CRFSuiteOutcomeIDReport
import de.tudarmstadt.ukp.dkpro.tc.crfsuite.writer.CRFSuiteDataWriter
import de.tudarmstadt.ukp.dkpro.tc.ml.ExperimentCrossValidation
import de.tudarmstadt.ukp.dkpro.tc.ml.ExperimentTrainTest
import de.tudarmstadt.ukp.dkpro.core.api.resources.DkproContext
import de.tudarmstadt.ukp.experiments.ej.repeatwithcrowdsource.pos.PosReaderBase
import de.tudarmstadt.ukp.experiments.ej.repeatwithcrowdsource.util.EJConstants

//import de.tudarmstadt.ukp.experiments.ej.repeatwithcrowdsource.pos.PosParams;
//import de.tudarmstadt.ukp.experiments.ej.repeatwithcrowdsource/src/main/resources/scripts/PosParams
//@BaseScript PosParams posParams

/**
 * This a Groovy experiment setup of POS tagging as sequence tagging.
 * Runtime is ~40 minutes with 50m clusters, no ngrams, and full crowdsourced gimbel dataset.
 * Note: The groovy file called by the java starter should be in /scripts but any inherited 
 * parent groovy classes should be in the package with the other regular java files.
 */
class PosSequenceExperiment //extends PosParams
implements Constants {
	
	def String experimentName;
	def String corpusNameTrain;
	def String corpusNameTest;
	def dimReaders;
    def languageCode = "en"
	def String origAnnosTrain;
	def String origAnnosTest;
    def String CorpusDirectory = DkproContext.getContext().getWorkspace("corpora").getAbsolutePath() + "/HovyTwitterPOS/";
	def instanceModeTrain = PosReaderBase.InstanceCreationModes.HIGHAGREE; //change
	def instanceModeTest = PosReaderBase.InstanceCreationModes.MAJVOTE; //keep
	def shrinkDatasetSizeForDebugging = 15000; //total size 14,619 instances
    def NUM_FOLDS = 5 //paper uses 5 because 10 is too computationally intensive
	
	def setDimReaders(){
		dimReaders =  Dimension.createBundle("readers", [
        readerTrain: PosReader,
        readerTrainParams: [
            PosReader.PARAM_LANGUAGE, languageCode,
            PosReader.PARAM_CORPUS_FILE_PATH, CorpusDirectory+corpusNameTrain,
			PosReader.PARAM_ORIGINAL_FILE, CorpusDirectory+origAnnosTrain,
			PosReader.PARAM_SHRINK_DATASET_SIZE_FOR_DEBUGGING, shrinkDatasetSizeForDebugging,
			PosReader.PARAM_REMOVE_BAD_TWEETS, false,
            PosReader.PARAM_INSTANCE_MODE, instanceModeTrain
        ],
		//readerTest only needed for train/test mode
		readerTest: PosReader,
        readerTestParams: [
            PosReader.PARAM_LANGUAGE, languageCode,
            PosReader.PARAM_CORPUS_FILE_PATH, CorpusDirectory+corpusNameTest,
			PosReader.PARAM_ORIGINAL_FILE, CorpusDirectory+origAnnosTest,
			PosReader.PARAM_REMOVE_BAD_TWEETS, false,
            PosReader.PARAM_INSTANCE_MODE, instanceModeTest
        ]])
		return
	}


    def dimLearningMode = Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL)
    def dimFeatureMode = Dimension.create(DIM_FEATURE_MODE, FM_SEQUENCE)
	def dimFeatureSets = Dimension.create(
		DIM_FEATURE_SET, [
	//		LuceneNGramUFE.name,
			AddWordClusterId.name,
			HovyTwitterPosTagUFE.name,
			AffixPosTagUFE.name
		])
	
	def dimPipelineParameters = Dimension.create(
		DIM_PIPELINE_PARAMS,
			[
				LuceneNGramUFE.PARAM_NGRAM_MAX_N, 1,
				LuceneNGramUFE.PARAM_NGRAM_USE_TOP_K, 500,
				// 1kpaths, 10kpaths, 100kpaths, 750kpaths, 3mpaths, 6mpaths, 50mpaths2.
				AddWordClusterId.PARAM_RESOUCE_FILELOC, CorpusDirectory + "50mpaths2"
			])
	
	def setExperimentName(String aName){
		experimentName = aName;
	}
	def setCorpusNameTrain(String aName){
		corpusNameTrain = aName;
	}
	def setCorpusNameTest(String aName){
		corpusNameTest = aName;
	}
	def setOrigFileNameTrain(String aName){
		origAnnosTrain = aName;
	}
	def setOrigFileNameTest(String aName){
		origAnnosTest = aName;
	}
	


    // ##### CV #####
    protected void runCrossValidation()
    throws Exception
    {
        ExperimentCrossValidation batchTask = [
            experimentName: experimentName + "-CV-Groovy",
            // we need to explicitly set the name of the batch task, as the constructor of the groovy setup must be zero-arg
            type: "Evaluation-"+ experimentName +"-CV-Groovy",
            preprocessing:  getPreprocessing(),
			machineLearningAdapter: CRFSuiteAdapter,
            innerReports: [CRFSuiteClassificationReport],
            parameterSpace : [
                dimReaders,
                dimFeatureMode,
                dimLearningMode,
                dimFeatureSets,
				dimPipelineParameters
            ],
            executionPolicy: ExecutionPolicy.RUN_AGAIN,
            reports:         [
				CRFSuiteBatchCrossValidationReport
            ],
            numFolds: NUM_FOLDS]

        // Run
        Lab.getInstance().run(batchTask)
    }
	
	/**
	 * TrainTest Setting
	 *
	 * @throws Exception
	 */
	protected void runTrainTest() throws Exception
	{
		ExperimentTrainTest batchTask = [
			experimentName: experimentName + "-TrainTest-Groovy",
			// we need to explicitly set the name of the batch task, as the constructor of the groovy setup must be zero-arg
			type: "Evaluation-"+ experimentName +"-TrainTest-Groovy",
			preprocessing:  getPreprocessing(),
			machineLearningAdapter: CRFSuiteAdapter,
			innerReports: [CRFSuiteClassificationReport],
			parameterSpace : [
				dimReaders,
				dimFeatureMode,
				dimLearningMode,
				dimFeatureSets,
				dimPipelineParameters
			],
			executionPolicy: ExecutionPolicy.RUN_AGAIN,
			reports:         [
//				CRFSuiteClassificationReport,
//				CRFSuiteOutcomeIDReport,
				CRFSuiteBatchTrainTestReport]
		];

		// Run
		Lab.getInstance().run(batchTask);
	}

    protected AnalysisEngineDescription getPreprocessing()
    throws ResourceInitializationException
    {
        return createEngineDescription(NoOpAnnotator)
    }

	public void runManualCVExperiment(){
		PosSequenceExperiment experiment = new PosSequenceExperiment();
		experiment.setExperimentName(EJConstants.POSSEQUENCEEXPERIMENT);
		//numFolds doesn't have to be max available, for dev purposes.
//		int numFolds = 10;
		for(int i=0;i<NUM_FOLDS;i++){
			experiment.setCorpusNameTrain("/CVFiles/gimpel_crowdsourced.tsv.r" + i + ".devtrain.txt");
			experiment.setCorpusNameTest("/CVFiles/gimpel_crowdsourced.tsv.r" + i + ".devtest.txt");
			experiment.setOrigFileNameTrain("/CVFiles/oct27train.r" + i + ".devtrain.txt");
			experiment.setOrigFileNameTest("/CVFiles/oct27train.r" + i + ".devtest.txt");
			experiment.setDimReaders();
			experiment.runTrainTest();
		}
	}
	
    public void run()
    {
		
		runManualCVExperiment();
		
    }
}
