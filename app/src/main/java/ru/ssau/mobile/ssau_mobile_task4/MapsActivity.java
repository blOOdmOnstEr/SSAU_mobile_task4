package ru.ssau.mobile.ssau_mobile_task4;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ru.ssau.mobile.ssau_mobile_task4.adapters.IconChooserAdapter;
import ru.ssau.mobile.ssau_mobile_task4.adapters.SearchAdapter;
import ru.ssau.mobile.ssau_mobile_task4.location.FusedLocationReceiver;
import ru.ssau.mobile.ssau_mobile_task4.location.FusedLocationService;
import ru.ssau.mobile.ssau_mobile_task4.location.PermissionUtils;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener {

    public static final int[] MARKER_ICONS = {R.drawable.blue1, R.drawable.blue2, R.drawable.blue3, R.drawable.blue4, R.drawable.blue5,
            R.drawable.blue6, R.drawable.blue7, R.drawable.red1, R.drawable.red2, R.drawable.red3, R.drawable.red4, R.drawable.red5,
            R.drawable.red6, R.drawable.red7, R.drawable.green1, R.drawable.green2, R.drawable.green3, R.drawable.green4, R.drawable.green5,
            R.drawable.green6, R.drawable.green7, R.drawable.orange1, R.drawable.orange2, R.drawable.orange3, R.drawable.orange4,
            R.drawable.orange5, R.drawable.orange6, R.drawable.orange7, R.drawable.blue_pin, R.drawable.red_pin, R.drawable.orange_pin, R.drawable.green_pin};

    public static final int[] TRAVEL_COLORS = {
            Color.rgb(64, 89, 128), Color.rgb(149, 165, 124), Color.rgb(217, 184, 162),
            Color.rgb(191, 134, 134), Color.rgb(179, 48, 80), Color.rgb(193, 37, 82),
            Color.rgb(255, 102, 0), Color.rgb(245, 199, 0), Color.rgb(106, 150, 31),
            Color.rgb(179, 100, 53), Color.rgb(192, 255, 140), Color.rgb(255, 247, 140),
            Color.rgb(255, 208, 140), Color.rgb(140, 234, 255), Color.rgb(255, 140, 157)
    };

    public int currentColor = 0;

    public static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private ArrayList<MarkerData> markers;
    private HashMap<String, MarkerData> idToData;
    private View markerOptions;
    private FloatingActionButton putMarkerFab;

    private LinearLayout focusDummy;

    private Marker locationMarker = null;
    Geocoder geocoder;
    FusedLocationService fusedLocationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        PermissionUtils.requestPermissions(this, 42, permissions);
        if (!isGooglePlayServicesAvailable()) {
            finish();
        }

        geocoder = new Geocoder(this);


        setContentView(R.layout.activity_maps);
        focusDummy = (LinearLayout) findViewById(R.id.focus_dummy);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        idToData = new HashMap<>();
        //test
        if (savedInstanceState == null) {
            LatLng sydney = new LatLng(-34, 151);
            MarkerOptions m = new MarkerOptions().position(sydney).title("Marker in Sydney");
            markers = new ArrayList<>();
            markers.add(new MarkerData(m));
        } else {
            markers = (ArrayList<MarkerData>) savedInstanceState.get("markers");
        }

        markerOptions = findViewById(R.id.marker_buttons);
        Button left = (Button) markerOptions.findViewById(R.id.marker_left_button);
        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: LEFT");
            }
        });
        Button right = (Button) markerOptions.findViewById(R.id.marker_right_button);
        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: RIGHT");
            }
        });
        //end

       final FusedLocationReceiver receiver = new FusedLocationReceiver() {
           @Override
           public void onLocationChanged() {
               hideMarkerOptions();
               Location location = fusedLocationService.getLocation();
               Log.d(TAG, "onLocationChanged: lat "+location.getLatitude() + " lng " + location.getLongitude());
               LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
               MarkerOptions m = new MarkerOptions().position(position).title("You are here!");
               Bitmap icon = BitmapFactory.decodeResource(getResources(),
                       R.drawable.blue_pin);
               icon = Bitmap.createScaledBitmap(icon, 100, 100, false);
               m = m.icon(BitmapDescriptorFactory.fromBitmap(icon));
               if (locationMarker != null)
                   locationMarker.remove();
               locationMarker = mMap.addMarker(m);
               locationMarker.showInfoWindow();
               putMarkerFab.setVisibility(View.VISIBLE);
               mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
           }
       };

        FloatingActionButton locateFab = (FloatingActionButton) findViewById(R.id.fab_locate);
        locateFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick FAB");
                fusedLocationService = new FusedLocationService(MapsActivity.this, receiver);
            }
        });

        putMarkerFab = (FloatingActionButton) findViewById(R.id.fab_put_marker);
        putMarkerFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MarkerData md = new MarkerData(locationMarker);
                md.setIconId(R.drawable.red1);
                md.title = "";
                try {
                    if(locationMarker.isVisible()) {
                        Log.d(TAG, "onClick: visible");
                        locationMarker.remove();
                    }
                    else
                        Log.d(TAG, "onClick: not visible");
                }catch (IllegalArgumentException e) {
                    Log.w(TAG, "onClick: IllegalArgumentException!");
                }
                locationMarker = null;
                Marker m = mMap.addMarker(md.getMarkerOptions(MapsActivity.this));
                markers.add(md);
                idToData.put(m.getId(), md);
                putMarkerFab.setVisibility(View.GONE);
            }
        });

        ImageButton zoomIn = (ImageButton) findViewById(R.id.zoom_in);
        zoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });
        ImageButton zoomOut = (ImageButton) findViewById(R.id.zoom_out);
        zoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });

        setUpSearch();
    }

    private void setUpSearch() {
        final AutoCompleteTextView searchView = (AutoCompleteTextView) findViewById(R.id.search_text);
        //ArrayAdapter<String> searchAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
//        SearchAdapter searchAdapter = new SearchAdapter(this, android.R.layout.simple_list_item_1);

        //searchView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line,
        //      new String[]{"lol1", "lal2", "lel3", "lul4"}));

        SearchAdapter searchAdapter = new SearchAdapter(this, android.R.layout.simple_dropdown_item_1line);
        searchView.setAdapter(searchAdapter);
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.d(TAG, "onTextChanged: "+charSequence);
                if (charSequence.length() > 1) {
                    ArrayList<String> results = new ArrayList<>();
                    try {
                        List<Address> addresses = geocoder.getFromLocationName(charSequence.toString(), 5);
                        for (Address a : addresses) {
                            int maxLine = a.getMaxAddressLineIndex();
                            String res = a.getAddressLine(0)+
                                    (maxLine > 0 ? ", "+a.getAddressLine(1)+
                                    (maxLine > 1 ? ", "+a.getAddressLine(2) : "") : "");
                            results.add(res);
                            Log.d(TAG, "onTextChanged: result = "+res);
                        }
                        //((SearchAdapter)searchView.getAdapter()).setData(results);
                        searchView.setAdapter(new ArrayAdapter<String>(MapsActivity.this, android.R.layout.simple_dropdown_item_1line, results));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        final ImageButton searchButton = (ImageButton) findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchButton.setVisibility(View.GONE);
                searchView.setVisibility(View.VISIBLE);
                if (searchView.requestFocus())
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
        });

        searchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (view instanceof AutoCompleteTextView && !b) {
                    searchView.setVisibility(View.GONE);
                    searchButton.setVisibility(View.VISIBLE);
                    //hideSoftKeyboard(MapsActivity.this);
                }
            }
        });
    }

    public void showMarkerOptions() {
        markerOptions.setVisibility(View.VISIBLE);
    }

    public void hideMarkerOptions() {
        markerOptions.setVisibility(View.GONE);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                hideSoftKeyboard(MapsActivity.this);
                focusDummy.requestFocus();
            }
        });
        mMap.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener() {
            @Override
            public void onInfoWindowClose(Marker marker) {
                hideMarkerOptions();
                if (locationMarker != null) {
                    locationMarker.remove();
                    locationMarker = null;
                    putMarkerFab.setVisibility(View.GONE);
                }
            }
        });

        for (MarkerData md : markers) {
            Marker m = mMap.addMarker(md.getMarkerOptions(this));
            idToData.put(m.getId(), md);
        }
        LatLng pos = markers.get(markers.size()-1).getPosition();
        mMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClick");
        marker.showInfoWindow();
        showMarkerOptions();

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View convertView = inflater.inflate(R.layout.icon_chooser, null);
        dialogBuilder.setView(convertView);
        dialogBuilder.setTitle("Choose marker icon");
        dialogBuilder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        dialogBuilder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        IconChooserAdapter adapter = new IconChooserAdapter(this, MARKER_ICONS, marker);
        GridView grid = (GridView) convertView.findViewById(R.id.icon_chooser);
        grid.setAdapter(adapter);
        dialogBuilder.show();

        /*
        MarkerData md = idToData.get(marker.getId());
        int pos = (int) (Math.random()*MARKER_ICONS.length);
        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                MARKER_ICONS[pos]);
        icon = Bitmap.createScaledBitmap(icon, 100, 100, false);
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(icon));
        md.setIconId(MARKER_ICONS[pos]);
        */
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("markers", markers);
        super.onSaveInstanceState(outState);
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GoogleApiAvailability.getInstance().showErrorDialogFragment(this, status, 0);
            return false;
        }
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }
}
