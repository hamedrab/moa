/*
 *    PreviewCollection.java
 *    Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tuan Pham Minh (tuan.pham@ovgu.de)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import moa.core.StringUtils;
import moa.tasks.active.ALCrossValidationTask;

/**
 * Class that stores and keeps the history of multiple previews
 *
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class PreviewCollection<CollectionElementType extends Preview> extends Preview {

	private static final long serialVersionUID = 1L;

	// the name representing the ordering
	String orderingName;
	// the name representing the index of each preview
	String indexName;
	// a list of all previews which should be stored
	List<CollectionElementType> subPreviews;
	// the measurement names a preview has to contain to be added in this
	// collection
	List<String> requiredMeasurementNames;
	// all measurement names used by this collection
	List<String> measurementNames;
	// the minimum of entries of all included previews
	int minEntryNum;
	// the type of Task which uses this preview collection
	Class<?> taskClass;
	// name of the varied parameter (can be null)
	String variedParamName;
	// values for the varied parameter (can be null)
	double[] variedParamValues;
	
	public PreviewCollection(String orderingName, String indexName, Class<?> taskClass, 
			String variedParamName, double[] variedParamValues) 
	{
		this.indexName = indexName;
		requiredMeasurementNames = new ArrayList<>();
		measurementNames = new ArrayList<>();
		measurementNames.add(orderingName);
		measurementNames.add(indexName);
		subPreviews = new ArrayList<>();
		this.taskClass = taskClass;
		this.variedParamName = variedParamName;
		this.variedParamValues = variedParamValues;
	}
	
	public PreviewCollection(String orderingName, String indexName, Class<?> taskClass) {
		this(orderingName, indexName, taskClass, null, null);
	}

	public void setPreview(int previewIndex, CollectionElementType preview) throws IllegalArgumentException {
		// ignore the preview if it has no entries
		if (preview.numEntries() > 0) {
			// copy the measurement names from the first preview
			if (subPreviews.size() == 0) {
				for (int i = 0; i < preview.getMeasurementNameCount(); ++i) {
					String name = preview.getMeasurementName(i);
					measurementNames.add(name);
					requiredMeasurementNames.add(name);
				}
			}

			// resize the array if new previews are added to the collection
			if (subPreviews.size() <= previewIndex) {
				if (subPreviews.size() < previewIndex) {
					throw new IndexOutOfBoundsException(
							"The given index (" + String.valueOf(previewIndex) + ") is invalid.");
				} else {
					subPreviews.add(null);
				}
			}

			// check if the measurement names are the same
			boolean hasSameMeasurementNamesCount = requiredMeasurementNames.size() == preview.getMeasurementNameCount();
			if (hasSameMeasurementNamesCount) {
				boolean hasSameMeasurementNames = true;
				for (int i = 0; i < requiredMeasurementNames.size(); ++i) {
					hasSameMeasurementNames &= requiredMeasurementNames.get(i).equals(preview.getMeasurementName(i));
				}

				if (hasSameMeasurementNames) {
					// check if the new preview has more entries than the last one
					if (subPreviews.get(previewIndex) == null ||
						(subPreviews.get(previewIndex) != null && 
						 preview.numEntries() > subPreviews.get(previewIndex).numEntries())) 
					{
						// set the smallest number of entries
						minEntryNum = preview.numEntries();
						for (int i = 0; i < subPreviews.size(); ++i) {
							if (i != previewIndex) {
								int entryNum = subPreviews.get(i).numEntries();
								minEntryNum = Math.min(minEntryNum, entryNum);
							}
						}
						// set the preview
						subPreviews.set(previewIndex, preview);
					}
					
				} else {
					throw new IllegalArgumentException("The measurement names of all previews have to be equal");
				}
			} else {
				throw new IllegalArgumentException("The number measurement names of all previews have to be equal");
			}

		}
	}

	public int numEntries() {
		// use the minimal number of entries to guarantee that all previews have
		// enough values
		return minEntryNum * subPreviews.size();
	}

	public String headerToString() {
		// append all measurement names separated by a comma
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String name : this.measurementNames) {
			if (!first) {
				sb.append(',');
			} else {
				first = false;
			}
			sb.append(name);
		}
		return sb.toString();
	}

	@Override
	public String entryToString(int entryIndex) {
		if (subPreviews.size() > 0) {
			return entryToString(entryIndex % subPreviews.size(), entryIndex / subPreviews.size());
		} else {
			return "";
		}
	}

	public String entryToString(int subPreviewIndex, int interlacedEntryIndex) {
		StringBuilder sb = new StringBuilder();
		// use the row index as ordering value
		int orderingValue = interlacedEntryIndex * subPreviews.size() + subPreviewIndex;
		// append the ordering value
		sb.append(orderingValue);
		sb.append(",");
		// append the index of the preview to differentiate between those
		sb.append(subPreviewIndex);
		sb.append(",");
		// append the content of the entry from the wanted preview
		sb.append(subPreviews.get(subPreviewIndex).entryToString(interlacedEntryIndex));
		return sb.toString();
	}

	public void getDescription(StringBuilder sb, int indent) {
		sb.append(headerToString());

		// iterate over all entries and previews to get an interlaced output
		for (int entryIdx = 0; entryIdx < numEntries(); entryIdx++) {
			StringUtils.appendNewlineIndented(sb, indent, entryToString(entryIdx));
		}
	}

	public List<CollectionElementType> getPreviews() {
		return subPreviews;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		getDescription(sb, 0);
		return sb.toString();
	}

	@Override
	public int getMeasurementNameCount() {
		return measurementNames.size();
	}

	@Override
	public String getMeasurementName(int measurementIndex) {
		return measurementNames.get(measurementIndex);
	}

	@Override
	public Class<?> getTaskClass() {
		return taskClass;
	}

	@Override
	public double[] getEntryData(int entryIndex) {
		// preallocate the array for the entry data
		double[] entry = new double[getMeasurementNameCount()];
		// get the number of previews in this collection to reduce the number of
		// calls for .size()
		int numSubPreviews = subPreviews.size();
		// calculate the index of the corresponding preview and the entry index
		// for that one
		int subPreviewIndex = entryIndex % numSubPreviews;
		int subPreviewEntryIndex = entryIndex / numSubPreviews;
		// get the entry of the preview
		double[] subPreviewEntry = subPreviews.get(subPreviewIndex).getEntryData(subPreviewEntryIndex);
		// fill the first two elements with the entry index and the index of the
		// preview
		entry[0] = entryIndex;
		entry[1] = subPreviewIndex;
		// fill the rest with the entry data of the preview
		for (int measurementIdx = 0; measurementIdx < subPreviewEntry.length; ++measurementIdx) {
			entry[2 + measurementIdx] = subPreviewEntry[measurementIdx];
		}

		return entry;
	}
	
	public String getOrderingName() {
		return this.orderingName;
	}
	
	public String getIndexName() {
		return this.indexName;
	}
	
	public String getVariedParamName() {
		return this.variedParamName;
	}
	
	public double[] getVariedParamValues() {
		return this.variedParamValues;
	}
	
	/**
	 * Calculate the averaged Preview Collection. The mean is calculated for each
	 * parameter value over all available folds. This function only has an 
	 * effect for PreviewCollections of PreviewCollections (for folds and 
	 * parameter values), simple PreviewCollections just return themselves.
	 * <br>
	 * Additionally, the standard deviations are calculated and appended to the
	 * list of measurements for each entry in each Preview.
	 * 
	 * @return PreviewCollection of mean Previews (one for each parameter value)
	 */
	@SuppressWarnings("unchecked")
	public PreviewCollection<Preview> calculateMeanPreview() 
	{
		if (this.subPreviews.isEmpty() || 
			!(this.subPreviews.get(0) instanceof PreviewCollection)) 
		{
			// There is only one set of previews with exactly one entry per
			// parameter value, so there is no other mean that could be 
			// calculated. Simply return this set.
			return (PreviewCollection<Preview>) this;
		}
		
		// create new preview collection for mean previews
		PreviewCollection<Preview> meanPreviews = 
				new PreviewCollection<Preview>(
						"mean preview entry id",
						"parameter value id",
						ALCrossValidationTask.class,
						this.variedParamName,
						this.variedParamValues);
		
		// calculate maximal number of entries that each Preview can provide
		int numFolds = this.subPreviews.size();
		int numParamValues = this.variedParamValues.length;
		int numEntriesPerPreview = this.numEntries() / numFolds / numParamValues;
				
		for (int paramValue = 0; paramValue < numParamValues; paramValue++)
		{
			Preview meanParamValuePreview = this.calculateMeanPreviewForParam(
							numEntriesPerPreview, numParamValues, paramValue);
			meanPreviews.setPreview(paramValue, meanParamValuePreview);
		}
		
		return meanPreviews;
	}
	
	/**
	 * Calculate the mean Preview for one specific parameter value.
	 * 
	 * @param numEntriesPerPreview
	 * @param numParamValues
	 * @param paramValue
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Preview calculateMeanPreviewForParam(
			int numEntriesPerPreview, int numParamValues, int paramValue) 
	{
		// initialize list for summing up all measurements
		List<double[]> paramMeasurementsSum = 
				new ArrayList<double[]>(numEntriesPerPreview);
		
		int numCompleteFolds = 0;
		
		for (CollectionElementType fold : this.subPreviews) {
			PreviewCollection<Preview> foldPreview = 
					(PreviewCollection<Preview>) fold;
			
			// check if there is a preview for each parameter value
			if (foldPreview.getPreviews().size() == numParamValues) {
				numCompleteFolds++;
				
				Preview foldParamPreview = 
						foldPreview.getPreviews().get(paramValue);
				
				// add this Preview's measurements to the overall sum
				this.addUpMeasurementEntries(
						paramMeasurementsSum, 
						foldParamPreview, 
						numEntriesPerPreview);
			}
		}
		
		// divide measurementsSum by number of folds to actually calculate mean
		for (double[] entry : paramMeasurementsSum) {
			for (int m = 0; m < entry.length; m++) {
				entry[m] /= numCompleteFolds;
			}
		}
		
		// add measurements for standard deviation
		this.addStandardDeviationMeasurementsForParam(
				paramMeasurementsSum, numCompleteFolds,
				numParamValues, paramValue, numEntriesPerPreview);
		
		// get actual measurement names (first four are only additional IDs)
		String[] cvMeasurementNames = this.getMeasurementNames();
		int numBaseMeasurementNames = cvMeasurementNames.length - 4;
		
		// copy old measurement names and append names for standard deviations
		String[] measurementNames = new String[numBaseMeasurementNames * 2];
		for (int m = 0; m < numBaseMeasurementNames; m++) {
			// copy old measurement names
			measurementNames[m] = cvMeasurementNames[m + 4];
			
			// append standard deviation measurement names
			measurementNames[m + numBaseMeasurementNames] =
					"[std] " + cvMeasurementNames[m + 4];
		}
		
		// wrap into LearningCurve
		LearningCurve meanLearningCurve = 
				new LearningCurve("learning evaluation instances");
		meanLearningCurve.setData(
				Arrays.asList(measurementNames), paramMeasurementsSum);
		
		// wrap into PreviewCollectionLearningCurveWrapper
		Preview meanParamValuePreview = 
				new PreviewCollectionLearningCurveWrapper(
						meanLearningCurve, this.taskClass);
		
		return meanParamValuePreview;
	}
	
	/**
	 * Add up the measurements of all different fold Previews for each entry.
	 * 
	 * @param measurementsSum
	 * @param preview
	 * @param maxEntries
	 */
	private void addUpMeasurementEntries(
			List<double[]> measurementsSum, Preview preview, int maxEntries) 
	{
		List<double[]> measurements = preview.getData();
		
		if (measurementsSum.isEmpty()) {
			// initialize measurement sums with values of first preview
			for (int entry = 0; entry < maxEntries; entry++) {
				measurementsSum.add(measurements.get(entry));
			}
		}
		else {
			// add values for each measurement in each entry
			for (int entry = 0; entry < maxEntries; entry++) {
				double[] entrySum = measurementsSum.get(entry);
				double[] previewEntry = measurements.get(entry);
				
				for (int measure = 0; measure < entrySum.length; measure++) {
					entrySum[measure] += previewEntry[measure];
				}
			}
		}
	}
	
	/**
	 * Add standard deviation values to the measurement collections for one
	 * specific parameter value.
	 * 
	 * @param meanMeasurements
	 * @param numCompleteFolds
	 * @param numParamValues
	 * @param paramValue
	 * @param numEntriesPerPreview
	 */
	@SuppressWarnings("unchecked")
	private void addStandardDeviationMeasurementsForParam(
			List<double[]> meanMeasurements, int numCompleteFolds,
			int numParamValues, int paramValue, int numEntriesPerPreview) 
	{
		int numInitialMeasurements = meanMeasurements.isEmpty() ? 0 : 
			meanMeasurements.get(0).length;
		
		for (CollectionElementType fold : this.subPreviews) {
			PreviewCollection<Preview> foldPreview = 
					(PreviewCollection<Preview>) fold;
			
			// check if there is a preview for each parameter value
			if (foldPreview.getPreviews().size() == numParamValues) {
				Preview foldParamPreview = 
						foldPreview.getPreviews().get(paramValue);
				
				// add this Preview's standardDeviations to the overall sum
				this.addUpMeasurementEntryStandardDeviations(
						meanMeasurements, 
						foldParamPreview, 
						numEntriesPerPreview,
						numInitialMeasurements);
			}
		}
		
		// divide measurementsSum by number of folds to actually calculate mean
		for (double[] entry : meanMeasurements) {
			for (int m = numInitialMeasurements; m < entry.length; m++) {
				entry[m] = Math.sqrt(entry[m]/numCompleteFolds);
			}
		}
	}
	
	/**
	 * Add up the squared deviations from the mean value over all folds for
	 * each entry.
	 * 
	 * @param meanMeasurements
	 * @param preview
	 * @param maxEntries
	 * @param numInitialMeasurements
	 */
	private void addUpMeasurementEntryStandardDeviations(
			List<double[]> meanMeasurements, Preview preview, int maxEntries,
			int numInitialMeasurements)
	{
		List<double[]> measurements = preview.getData();
		
		// add standard deviation for each measurement in each entry
		for (int entry = 0; entry < maxEntries; entry++) {
			double[] entryMeasurements = meanMeasurements.get(entry);
			double[] previewEntry = measurements.get(entry);
			
			if (entryMeasurements.length == numInitialMeasurements) {
				// add spaces for standard deviation measures
				entryMeasurements = 
						Arrays.copyOf(entryMeasurements, 
									  entryMeasurements.length*2);
			}
			
			for (int m = 0; m < numInitialMeasurements; m++) {
				double diff = (entryMeasurements[m] - previewEntry[m]);
				double var = diff * diff;
				entryMeasurements[m + numInitialMeasurements] += var;
			}
			
			meanMeasurements.set(entry, entryMeasurements);
		}
	}
}
