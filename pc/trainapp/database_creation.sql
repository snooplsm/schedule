CREATE TABLE agency_transfer_edge (
		agency_source varchar(2) NOT NULL,
		agency_target varchar(2) NOT NULL,
		source varchar(20) NOT NULL,
		target varchar(20) NOT NULL,
		duration integer
);
CREATE TABLE nested_shortest_path (
		id INT AUTO_INCREMENT PRIMARY KEY,
		lft INT NOT NULL,
		rgt INT NOT NULL,
		nodes TEXT
);
CREATE TABLE nested_trip (
        id INT AUTO_INCREMENT PRIMARY KEY,
		trip_id VARCHAR(20) NOT NULL,
        stop_id VARCHAR(20) NOT NULL,
		service_id VARCHAR(20) NOT NULL,
		depart VARCHAR(10),
		arrive VARCHAR(10),
        lft INT NOT NULL,
        rgt INT NOT NULL
);
CREATE TABLE service (
		service_id VARCHAR(20) NOT NULL,
		date varchar(8)
);
CREATE TABLE shortest_path (
		source VARCHAR(20) NOT NULL,
        target VARCHAR(20) NOT NULL,
		nodes TEXT
);
CREATE TABLE shortest_route_path (
		source VARCHAR(20) NOT NULL,
        target VARCHAR(20) NOT NULL,
		nodes TEXT,
		hop_count integer
);
CREATE TABLE station_route (
		station VARCHAR(20) NOT NULL,
		route VARCHAR(20)
);
CREATE TABLE stop (
		stop_id VARCHAR(20) NOT NULL,
		name varchar(150),
        lat integer,
		lon integer
);
CREATE TABLE stop_abbreviations (
		abbreviation VARCHAR(20) NOT NULL,
		total integer
);
CREATE TABLE transfer_edge (
		source varchar(20) NOT NULL,
		target varchar(20) NOT NULL,
		duration integer
);