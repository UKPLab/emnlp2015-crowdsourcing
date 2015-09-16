package de.tudarmstadt.ukp.experiments.ej.repeatwithcrowdsource.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.tc.api.exception.TextClassificationException;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;
import de.tudarmstadt.ukp.dkpro.tc.api.features.PairFeatureExtractor;

public abstract class AddResourceInfoPFEBase
	extends AddResourceInfoFEBase
	implements PairFeatureExtractor
{


	protected abstract String getViewID(JCas view1, JCas view2);

	@Override
	public List<Feature> extract(JCas view1, JCas view2)
			throws TextClassificationException
	{
//		DocumentMetaData dmd = DocumentMetaData.get(view1);
		String resourceValue = myResource.get(getViewID(view1, view2));

		
		List<Feature> featList = new ArrayList<Feature>();
		for(String val: allPossibleFeatures){
			if(val.equals(resourceValue)){
				featList.add(new Feature("RteTask_" + val, 1));
//				System.out.println("Added Feature: " + featureName + val + ", 1");
			}else{
				featList.add(new Feature("RteTask_" + val, 0));
//				System.out.println("Added Feature: " + featureName + val + ", 0");
			}
		}
//		System.out.println("------");
		return featList;
	}
	

}
