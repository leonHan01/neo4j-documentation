/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.index.impl.lucene;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexProvider;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public class Inserter
{
	public static void main( String[] args )
	{
		String path = args[0];
		final GraphDatabaseService db = new EmbeddedGraphDatabase( path );
		final IndexProvider provider = new LuceneIndexProvider( db );
		final Index<Node> index = provider.nodeIndex( "myIndex", LuceneIndexProvider.EXACT_CONFIG );
		final String[] keys = new String[] { "apoc", "zion", "morpheus" };
		final String[] values = new String[] { "hej", "yo", "something", "just a value", "anything" };
		
		for ( int i = 0; i < 5; i++ )
		{
			new Thread()
			{
				@Override
				public void run()
				{
					while ( true )
					{
						Transaction tx = db.beginTx();
						try
						{
							for ( int i = 0; i < 100; i++ )
							{
								Node node = db.createNode();
								index.add( node, keys[i%keys.length], values[i%values.length]+i );
							}
							tx.success();
						}
						finally
						{
							tx.finish();
						}
					}
				}
			}.start();
		}
	}
}
