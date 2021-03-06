/**
 * Copyright (c) 2017-2018, FinancialForce.com, inc
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 *   are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 * - Neither the name of the FinancialForce.com, inc nor the names of its contributors
 *      may be used to endorse or promote products derived from this software without
 *      specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 *  THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 *  OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 *  OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 **/

package com.financialforce.orizuru;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;

import com.financialforce.orizuru.exception.OrizuruException;
import com.financialforce.orizuru.exception.consumer.decode.DecodeTransportException;
import com.financialforce.orizuru.exception.consumer.handler.HandleMessageException;
import com.financialforce.orizuru.interfaces.IConsumer;
import com.financialforce.orizuru.interfaces.IPublisher;
import com.financialforce.orizuru.message.Context;
import com.financialforce.orizuru.message.Message;
import com.financialforce.orizuru.transport.Transport;

/**
 * Handles a message containing a FinancialForce Orizuru Avro Transport schema.
 * 
 * <p>
 * The Transport schema is in the form:
 * 
 * <pre>
 * <code>
 * {
 *     "namespace": "com.financialforce.orizuru",
 *     "name": "Transport",
 *     "type": "record",
 *     "fields": [
 * 	       { "name": "contextSchema", "type": "string" },
 * 		   { "name": "contextBuffer", "type": "bytes" },
 * 		   { "name": "messageSchema", "type": "string" },
 * 		   { "name": "messageBuffer", "type": "bytes" }
 * 	    ]
 * }
 * </code>
 * </pre>
 */
public abstract class AbstractConsumer<I extends GenericContainer, O extends GenericContainer> implements IConsumer {

	private static Schema transportSchema = Transport.getClassSchema();

	protected IPublisher<O> publisher = null;

	private String queueName = null;

	public AbstractConsumer(String queueName) {
		this.queueName = queueName;
	}

	/* (non-Javadoc)
	 * @see com.financialforce.orizuru.interfaces.IConsumer#consume(byte[])
	 */
	@Override
	public byte[] consume(byte[] body) throws OrizuruException {

		// Handle the input
		Transport transport = decodeTransport(body);

		Context context = new Context();
		context.decodeFromTransport(transport);

		Message incomingMessage = new Message();
		incomingMessage.decodeFromTransport(transport);

		I input = incomingMessage.decode();

		// Handle the message
		O outgoingMessage = handleMessage(context, input);

		// If a publisher is defined then create the message to publish
		if (publisher != null) {
			return publisher.publish(context, outgoingMessage);
		}

		return null;

	}

	public abstract O handleMessage(Context context, I input) throws HandleMessageException;

	@Override
	public String getQueueName() {
		return queueName;
	}

	// private methods

	private Transport decodeTransport(byte[] body) throws DecodeTransportException {

		try {

			DatumReader<Transport> transportDatumReader = new SpecificDatumReader<Transport>(transportSchema);
			BinaryDecoder transportDecoder = DecoderFactory.get().binaryDecoder(body, null);
			return transportDatumReader.read(null, transportDecoder);

		} catch (Exception ex) {
			throw new DecodeTransportException(ex);
		}

	}

}
