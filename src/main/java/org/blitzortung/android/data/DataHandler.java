package org.blitzortung.android.data;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import org.blitzortung.android.app.Preferences;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.provider.DataProvider;
import org.blitzortung.android.data.provider.DataProviderType;
import org.blitzortung.android.data.provider.DataResult;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DataHandler implements OnSharedPreferenceChangeListener {

	private final Lock lock = new ReentrantLock();

    private PackageInfo pInfo;
	private DataProvider dataProvider;

	private String username;
	private String password;

	private ProgressBar progressBar;
	private ImageView errorIndicator;

    private final Parameters parameters;

	private DataListener listener;

    public static class UpdateTargets {

		boolean updateStrokes;

		boolean updateStations;

		public UpdateTargets() {
			updateStrokes = false;
			updateStations = false;
		}

		public void addStrokes() {
			updateStrokes = true;
		}

		public boolean updateStrokes() {
			return updateStrokes;
		}

		public void addParticipants() {
			updateStations = true;
		}

		public boolean updateParticipants() {
			return updateStations;
		}

		public boolean anyUpdateRequested() {
			return updateStrokes || updateStations;
		}
	}

	public DataHandler(SharedPreferences sharedPreferences, PackageInfo pInfo) {
        parameters = new Parameters();
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        this.pInfo = pInfo;

		onSharedPreferenceChanged(sharedPreferences, Preferences.DATA_SOURCE_KEY);
		onSharedPreferenceChanged(sharedPreferences, Preferences.USERNAME_KEY);
		onSharedPreferenceChanged(sharedPreferences, Preferences.PASSWORD_KEY);
		onSharedPreferenceChanged(sharedPreferences, Preferences.RASTER_SIZE_KEY);
		onSharedPreferenceChanged(sharedPreferences, Preferences.REGION_KEY);
        onSharedPreferenceChanged(sharedPreferences, Preferences.INTERVAL_DURATION_KEY);
        onSharedPreferenceChanged(sharedPreferences, Preferences.HISTORIC_TIMESTEP_KEY);
	}

	private class FetchDataTask extends AsyncTask<Integer, Integer, DataResult> {

		protected void onProgressUpdate(Integer... progress) {
			Log.v("RetrieveStrokesTask", String.format("update progress %d", progress[0]));
		}

		protected void onPostExecute(DataResult result) {
			if (!result.hasFailed() && listener != null) {
				listener.onDataUpdate(result);
			}

			if (!result.processWasLocked()) {
				progressBar.setVisibility(View.INVISIBLE);
				progressBar.setProgress(progressBar.getMax());

				errorIndicator.setVisibility(result.hasFailed() ? View.VISIBLE : View.INVISIBLE);
			}
		}

		@Override
		protected DataResult doInBackground(Integer... params) {
            int intervalDuration = params[0];
            int intervalOffset = params[1];
            int rasterBaselength = params[2];
            int region = params[3];

            DataResult result = new DataResult();

            if (lock.tryLock()) {
				try {
					dataProvider.setUp();
					dataProvider.setCredentials(username, password);
					
					List<AbstractStroke> strokes;
					if (rasterBaselength == 0) {
						result.setIncremental();
						strokes = dataProvider.getStrokes(intervalDuration, intervalOffset);
					} else {
						strokes = dataProvider.getStrokesRaster(intervalDuration, rasterBaselength, intervalOffset, region);
					}
                    Parameters parameters = new Parameters();
                    parameters.setIntervalDuration(intervalDuration);
                    parameters.setIntervalOffset(intervalOffset);
                    parameters.setRegion(region);
                    parameters.setRasterBaselength(rasterBaselength);

                    result.setParameters(parameters);
                    result.setReferenceTime(System.currentTimeMillis());
                    result.setStrokes(strokes);
					result.setRasterParameters(dataProvider.getRasterParameters());
                    result.setHistogram(dataProvider.getHistogram());

					if (params.length > 4) {
                        boolean updateParticipants = (params[4] == 1);
                        if (updateParticipants) {
						result.setParticipants(dataProvider.getStations(region));
                        }
                    }

					dataProvider.shutDown();
				} catch (RuntimeException e) {
					e.printStackTrace();
				} finally {
					lock.unlock();
				}
			} else {
				result.setProcessWasLocked();
			}
			return result;
		}
	}

	public void updateData(UpdateTargets updateTargets) {
		progressBar.setVisibility(View.VISIBLE);
		progressBar.setProgress(0);
		
		boolean updateParticipants = false;
		if (updateTargets.updateParticipants()) {
			if (dataProvider.getType() == DataProviderType.HTTP || parameters.getRasterBaselength() == 0) {
				updateParticipants = true;
			}
		}

        new FetchDataTask().execute(parameters.getIntervalDuration(), parameters.getIntervalOffset(), dataProvider.getType() == DataProviderType.HTTP ? 0 : parameters.getRasterBaselength(), parameters.getRegion(), updateParticipants ? 1 : 0);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(Preferences.DATA_SOURCE_KEY)) {
			String providerTypeString = sharedPreferences.getString(Preferences.DATA_SOURCE_KEY, DataProviderType.RPC.toString());
			DataProviderType providerType = DataProviderType.valueOf(providerTypeString.toUpperCase());
			dataProvider = providerType.getProvider();
            dataProvider.setPackageInfo(pInfo);
            notifyDataReset();
        } else if (key.equals(Preferences.USERNAME_KEY)) {
			username = sharedPreferences.getString(Preferences.USERNAME_KEY, "");
		} else if (key.equals(Preferences.PASSWORD_KEY)) {
			password = sharedPreferences.getString(Preferences.PASSWORD_KEY, "");
		} else if (key.equals(Preferences.RASTER_SIZE_KEY)) {
			parameters.setRasterBaselength(Integer.parseInt(sharedPreferences.getString(Preferences.RASTER_SIZE_KEY, "10000")));
		} else if (key.equals(Preferences.INTERVAL_DURATION_KEY)) {
            parameters.setIntervalDuration(Integer.parseInt(sharedPreferences.getString(Preferences.INTERVAL_DURATION_KEY, "120")));
            dataProvider.reset();
            notifyDataReset();
        } else if (key.equals(Preferences.HISTORIC_TIMESTEP_KEY)) {
            parameters.setOffsetIncrement(Integer.parseInt(sharedPreferences.getString(Preferences.HISTORIC_TIMESTEP_KEY, "30")));
        } else if (key.equals(Preferences.REGION_KEY)) {
			parameters.setRegion(Integer.parseInt(sharedPreferences.getString(Preferences.REGION_KEY, "1")));
			dataProvider.reset();
            notifyDataReset();
		}

		if (dataProvider != null) {
			dataProvider.setCredentials(username, password);
		}
	}

    private void notifyDataReset() {
        if (listener != null) {
            listener.onDataReset();
        }
    }

	private static int storedRasterSize;

	public void toggleExtendedMode() {
		if (parameters.getRasterBaselength() > 0) {
			storedRasterSize = parameters.getRasterBaselength();
			parameters.setRasterBaselength(0);
		} else {
            parameters.setRasterBaselength(storedRasterSize);
		}
	}

	public void setProgressBar(ProgressBar progressBar) {
		this.progressBar = progressBar;
	}

	public void setErrorIndicator(ImageView errorIndicator) {
		this.errorIndicator = errorIndicator;
	}

	public void setDataListener(DataListener listener) {
		this.listener = listener;
	}

    public int getIntervalDuration() {
        return parameters.getIntervalDuration();
    }

    public boolean ffwdInterval() {
        return parameters.ffwdInterval();
    }

    public boolean rewInterval() {
        return parameters.revInterval();
    }

    public boolean goRealtime() {
        return parameters.goRealtime();
    }

    public boolean isRealtime() {
        return parameters.isRealtime();
    }

    public boolean matches(Parameters resultParameters) {
        return parameters.equals(resultParameters);
    }

}