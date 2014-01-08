package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.EventDetailsActivity;
import be.digitalia.fosdem.adapters.EventsAdapter;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.SimpleCursorLoader;
import be.digitalia.fosdem.model.Event;

/**
 * Bookmarks list, optionally filterable.
 * 
 * @author Christophe Beyls
 * 
 */
public class BookmarksListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private static final int BOOKMARKS_LOADER_ID = 1;
	private static final String PREF_UPCOMING_ONLY = "bookmarks_upcoming_only";

	private EventsAdapter adapter;
	private boolean upcomingOnly;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		adapter = new EventsAdapter(getActivity());
		setListAdapter(adapter);

		upcomingOnly = getActivity().getPreferences(Context.MODE_PRIVATE).getBoolean(PREF_UPCOMING_ONLY, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.no_bookmark));
		setListShown(false);

		getLoaderManager().initLoader(BOOKMARKS_LOADER_ID, null, this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.bookmarks, menu);
		menu.findItem(R.id.filter).setIcon(upcomingOnly ? R.drawable.ic_action_filter_selected : R.drawable.ic_action_filter);
		menu.findItem(R.id.upcoming_only).setChecked(upcomingOnly);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.upcoming_only:
			upcomingOnly = !upcomingOnly;
			FragmentActivity activity = getActivity();
			activity.supportInvalidateOptionsMenu();
			activity.getPreferences(Context.MODE_PRIVATE).edit().putBoolean(PREF_UPCOMING_ONLY, upcomingOnly).commit();
			getLoaderManager().restartLoader(BOOKMARKS_LOADER_ID, null, this);
			return true;
		}
		return false;
	}

	private static class BookmarksLoader extends SimpleCursorLoader {

		// Events that just started are still shown for 5 minutes
		private static final long TIME_OFFSET = 5L * 60L * 1000L;

		private final boolean upcomingOnly;
		private final Handler handler;
		private final Runnable timeoutRunnable = new Runnable() {

			@Override
			public void run() {
				onContentChanged();
			}
		};

		public BookmarksLoader(Context context, boolean upcomingOnly) {
			super(context);
			this.upcomingOnly = upcomingOnly;
			this.handler = new Handler();
		}

		@Override
		public void deliverResult(Cursor cursor) {
			if (upcomingOnly && !isReset()) {
				handler.removeCallbacks(timeoutRunnable);
				// The loader will be refreshed when the start time of the first bookmark in the list is reached
				if ((cursor != null) && cursor.moveToFirst()) {
					long startTime = DatabaseManager.toEventStartTimeMillis(cursor);
					if (startTime != -1L) {
						long delay = startTime - (System.currentTimeMillis() - TIME_OFFSET);
						if (delay > 0L) {
							handler.postDelayed(timeoutRunnable, delay);
						} else {
							onContentChanged();
						}
					}
				}
			}
			super.deliverResult(cursor);
		}

		@Override
		protected void onReset() {
			super.onReset();
			if (upcomingOnly) {
				handler.removeCallbacks(timeoutRunnable);
			}
		}

		@Override
		protected Cursor getCursor() {
			return DatabaseManager.getInstance().getBookmarks(upcomingOnly ? System.currentTimeMillis() - TIME_OFFSET : -1L);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new BookmarksLoader(getActivity(), upcomingOnly);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (data != null) {
			adapter.swapCursor(data);
		}

		// The list should now be shown.
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Event event = adapter.getItem(position);
		Intent intent = new Intent(getActivity(), EventDetailsActivity.class).putExtra(EventDetailsActivity.EXTRA_EVENT, event);
		startActivity(intent);
	}
}
