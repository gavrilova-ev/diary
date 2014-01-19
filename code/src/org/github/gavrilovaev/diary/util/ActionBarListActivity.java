package org.github.gavrilovaev.diary.util;

import android.support.v7.app.ActionBarActivity;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ActionBarListActivity extends ActionBarActivity {

	private ListView listView;

	protected ListView getListView() {
		if (listView == null) {
			listView = (ListView) findViewById(android.R.id.list);
		}
		return listView;
	}

	protected void setListAdapter(ListAdapter adapter) {
		getListView().setAdapter(adapter);
	}

	protected ListAdapter getListAdapter() {
		ListAdapter adapter = getListView().getAdapter();
		if (adapter instanceof HeaderViewListAdapter) {
			return ((HeaderViewListAdapter) adapter).getWrappedAdapter();
		} else {
			return adapter;
		}
	}
}
